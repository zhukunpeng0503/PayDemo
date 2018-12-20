package com.zkp.walletdemo.utils.network;

import android.util.Log;

import com.zkp.walletdemo.Encryptor;
import com.zkp.walletdemo.utils.json.Object2Json;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class Post {
    private static final String TAG = "Post";

	public static final int CONNECT_TIMEOUT_TIME = 25 * 1000;
	public static final int GETDATA_TIMEOUT_TIME = 45 * 1000;

    public static final int K = 1024;
    public static final int INPUTBUFFSIZE = 8 * K;

    public static NetResult post(HashMap<String, Object> paramMap, String urlString,
                                 String savePath, Encryptor encryptor) {
        return post(Object2Json.map2Json(paramMap), urlString, savePath,
                encryptor);
    }

    public static NetResult post(String contentType, HashMap<String, Object> paramMap, String urlString,
                                 String savePath, Encryptor encryptor) {
        return post(contentType, Object2Json.map2Json(paramMap), urlString, savePath,
                encryptor);
    }

    public static NetResult post(String requestEntity, String urlString, String savePath,
                                 Encryptor encryptor) {
        return post("application/json;charset=UTF-8", requestEntity, urlString, savePath, encryptor);
    }

    public static NetResult post(String contentType, String requestEntity, String urlString, String savePath,
                                 Encryptor encryptor) {
        LinkedHashMap<String, List<String>> head = new LinkedHashMap<String, List<String>>();
        head.put("Accept", new ArrayList<String>(Arrays.asList("*/*")));
        head.put("Connection", new ArrayList<String>(Arrays.asList("Keep-Alive")));
        head.put("Accept-Language", new ArrayList<String>(Arrays.asList("zh-CN")));
        head.put("Content-type", new ArrayList<String>(Arrays.asList(contentType)));
        return post(head, requestEntity, urlString, savePath, encryptor);
    }



    public static NetResult post(Map<String, List<String>> headMap, String requestEntity, String urlString, String savePath,
                                 Encryptor encryptor) {

//		if (LibDebug.ENABLE_JRE_MODE) {
//			savePath = savePath == null ? null : "D:/协议测试"
//					+ savePath.substring(savePath.lastIndexOf("/"),
//							savePath.length());
//		}

        String name = "";

//		if (LibDebug.isLogOn) {
//			if (!StringUtils.isEmpty(urlString)) {
//				int vindex = urlString.lastIndexOf("/v");
//				if (vindex == -1) {
//					vindex = urlString.lastIndexOf("/");
//				}
//				name = urlString.substring(vindex) + " | ";
//			}
//		}

        if (requestEntity != null) { // 如果输入数据需要加密
            if (encryptor != null) {
//				if (LibDebug.isLogOn) {
                Log.i(TAG, name + "请求原文：" + requestEntity);
                Log.i(TAG, "加密...");
//				}
                requestEntity = encryptor.encrypt(requestEntity);
            } else {
//				if (LibDebug.isLogOn) {
                Log.i(TAG, "无加密器");
//				}
            }
        }

        boolean save = savePath != null;

//		if (LibDebug.isLogOn) {
        Log.i(TAG, name + "请求：");
        Log.i(TAG, name + "url =  " + urlString);
        Log.i(TAG, name + "post = " + requestEntity);
//			if (save) {
//				LogUtils.logi(TAG, name + "文件 = " + savePath);
//			}
//		}

        NetResult netResult = new NetResult();
        String strResult = null;

        HttpURLConnection conn = null;
        DataOutputStream postStream = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedWriter writer = null;

        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_TIME);
            conn.setReadTimeout(GETDATA_TIMEOUT_TIME);
            fillRequestProperties(conn, headMap);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false); // post 请求不能使用缓存
            conn.setInstanceFollowRedirects(true);

            conn.connect();

            postStream = new DataOutputStream(conn.getOutputStream());
            writer = new BufferedWriter(new OutputStreamWriter(postStream,
                    "UTF-8"));
            writer.write(requestEntity);
            // postStream.writeBytes(requestEntity);
            // postStream.flush();
            writer.flush();
            try {
                postStream.close();
            } catch (Exception e) {
//				if (LibDebug.isLogOn) {
                Log.i(
                        TAG,
                        name + "postStream.close() error: "
                                + e);
//				}
            }
            try {
                writer.close();
            } catch (Exception e) {
//				if (LibDebug.isLogOn) {
//					LogUtils.loge(TAG,
//							name + "writer.close() error: " + e);
//				}
            }

            try {
                netResult.mHeaderFields = conn.getHeaderFields();
            } catch (Exception e1) {
//				if (LibDebug.isLogOn) {
//					Log.i(
//							TAG,
//							"post(head, requestEntity, urlString, savePath, encryptor)",
//							"getHeaderFields Exception: " + e1);
//				}
            }

            int responseCode = conn.getResponseCode();
//			if (LibDebug.isLogOn) {
//				int conLen;
//				try {
//					conLen = conn.getContentLength();
//				}
//				catch (Exception e) {
//					conLen = -9527;
//				}
//				LogUtils.log(TAG,
//						"post(head, requestEntity, urlString, savePath, encryptor)",
//						"getContentLength = " + conLen);
//			}
            if (responseCode == 200) {
//				if (LibDebug.isLogOn) {
                Log.i(TAG, name + "responseCode = " + responseCode);
//				}
                is = conn.getInputStream();
                bis = new BufferedInputStream(is, INPUTBUFFSIZE);
                bis.mark(INPUTBUFFSIZE);

                if (save) {
                    fos = new FileOutputStream(savePath);

                    byte[] buf = new byte[1024];
                    int count = -1, length = 0;
                    while ((count = bis.read(buf)) != -1) {
                        fos.write(buf, 0, count);
                        length++;
                    }

                    fos.close();

                    if (length > INPUTBUFFSIZE) {
                        strResult = convertStreamToString(new FileInputStream(
                                savePath), -1);
//						if (LibDebug.isLogOn) {
//							LogUtils.logi(TAG, name + "大文件:");
//						}
                    } else {
                        bis.reset();
                        strResult = convertStreamToString(bis, length);
//						if (LibDebug.isLogOn) {
//							LogUtils.logi(TAG, name + "小文件:");
//						}
                    }
                } else {
                    strResult = convertStreamToString(bis, -1);
//					if (LibDebug.isLogOn) {
//						LogUtils.logi(TAG, name + "未保存文件,");
//					}
                }

            } else {
//				if (LibDebug.isLogOn) {
					Log.i(TAG, name + "responseCode = " + responseCode);
//				}
            }
        } catch (Exception e) {
//			if (LibDebug.isLogOn) {
				Log.i(TAG, name + "post error: " + e);
//			}
        } finally {
            if (postStream != null) {
                try {
                    postStream.close();
                } catch (Exception e) {
//					if (LibDebug.isLogOn) {
//						LogUtils.loge(TAG, name + "postStream.close() error: "
//								+ e);
//					}
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
//					if (LibDebug.isLogOn) {
//						LogUtils.loge(TAG, name + "writer.close() error: " + e);
//					}
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
//					if (LibDebug.isLogOn) {
//						LogUtils.loge(TAG, name + "is.close() error: " + e);
//					}
                }
            }
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
//					if (LibDebug.isLogOn) {
//						LogUtils.log("Post", "post()","Exception: " + e);
//					}
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
//					if (LibDebug.isLogOn) {
//						LogUtils.loge(TAG, name + "fos.close() error: " + e);
//					}
                }
            }
        }

        try {
            if (strResult != null) {
                if (encryptor != null) {
//					if (LibDebug.isLogOn) {
                    Log.i(TAG, "返回数据原始内容:" + strResult);
                    Log.i(TAG, "解密...");
//					}
                    strResult = encryptor.decrypt(strResult);
                } else {
//					if (LibDebug.isLogOn) {
                    Log.i(TAG, "无解密器");
//					}
                }
            } else {
//				if (LibDebug.isLogOn) {
                Log.i(TAG, name + "获取数据失败！");
//				}
            }
        } catch (Exception e) {
            strResult = null;
//			if (LibDebug.isLogOn) {
//				LogUtils.log(
//						TAG,
//						"post(headMap, requestEntity, urlString, savePath, encryptor)",
//						"Exception: " + e);
//			}
        }

//		if (LibDebug.isLogOn) {
//			if (StringUtils.isEmpty(strResult)) {
//				LogUtils.logi(TAG, name + "返回数据大小：" + -1 + "字节");
//			}
//			else {
//				LogUtils.logi(TAG, name + "返回数据大小："
//						+ strResult.getBytes().length + "字节");
//			}
//
			Log.i(TAG, name + "返回数据内容： " + strResult);
//		}

        netResult.mData = strResult;
        return netResult;
    }

    public static String convertStreamToString(InputStream input) throws Exception {
        return convertStreamToString(input, -1);
    }

    public static String convertStreamToString(InputStream input, int size)
            throws Exception {
        BufferedReader reader = null;
        if (size > 0) {
            reader = new BufferedReader(new InputStreamReader(input), size);
        } else {
            reader = new BufferedReader(new InputStreamReader(input));
        }
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        input.close();
        return sb.toString();
    }

    public static void fillRequestProperties(HttpURLConnection conn, Map<String, List<String>> headMap) throws Exception {
//		if (LibDebug.isLogOn) {
//			LogUtils.log(TAG, "fillRequestProperties(conn, headMap)", "headMap = " + Object2Json.toJson(headMap, true));
//		}

        if (headMap == null || headMap.size() < 1) {
            return;
        }

        Iterator<Entry<String, List<String>>> iterator = headMap.entrySet().iterator();
        Entry<String, List<String>> entry;
        while (iterator.hasNext()) {
            entry = iterator.next();
            String key = entry.getKey();
            List<String> valueList = entry.getValue();
            StringBuilder builder = new StringBuilder();
            if (valueList != null) {
                for (int i = 0; i < valueList.size(); i++) {
                    builder.append(valueList.get(i)).append(";");
                }
            }
            String value = builder.toString();
            if (value.endsWith(";")) {
                value = value.substring(0, value.length() - 1);
            }
            conn.setRequestProperty(key, value);
        }
    }
}
