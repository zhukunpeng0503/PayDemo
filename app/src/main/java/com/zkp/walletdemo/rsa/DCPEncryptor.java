package com.zkp.walletdemo.rsa;



import com.zkp.walletdemo.platform.Base64Comp;

import java.util.HashMap;


public class DCPEncryptor {

    /**
     * 验证签名
     *
     * @param signature        签名
     * @param signType         签名类型
     * @param key
     * @param iv
     * @param cipher
     * @param partnerPublicKey 商户公钥
     * @return
     */
    public static boolean verifySignature(String signature, String signType, String key, String iv, String cipher, String partnerPublicKey) {

        String signToken = cipher + key + iv;

        DCPRSA.DCPRSASignAlgorithm signAlgorithm = DCPRSA.DCPRSASignAlgorithm.RSA2;
        if (signType.equals("RSA")) {
            signAlgorithm = DCPRSA.DCPRSASignAlgorithm.RSA1;
        }

        byte[] tokenBytes = signToken.getBytes();
        byte[] signatureBytes = Base64Comp.decode(signature);

        try {
            return DCPRSA.verify(tokenBytes, signatureBytes, partnerPublicKey, signAlgorithm);
        } catch (Exception e) {
//            if (LibDebug.isLogOn) {
//                LogUtils.log("DCPEncryptor", "verifySignature()", "Exception: " + e);
//            }
            return false;
        }
    }

    public static byte[] decrypt(String key, String iv, String cipher, String selfPrivateKey) {
        try {
            //1. RSA解开AESKEY
            byte[] keyBytes = Base64Comp.decode(key);
            byte[] aesKeyPlain = DCPRSA.decrypt(keyBytes, selfPrivateKey);

            String aesKeyPlainBase64 = Base64Comp.encodeToString(aesKeyPlain);
            //System.out.println("aesKeyPlainBase64:"+aesKeyPlain.length);
            //System.out.println(aesKeyPlainBase64);

            //2. AES解开cipher
            byte[] ivBytes = Base64Comp.decode(iv);
            byte[] cipherBytes = Base64Comp.decode(cipher);

            return DCPAES.decrypt(aesKeyPlain, cipherBytes, ivBytes);

        } catch (Exception e) {
//            if (LibDebug.isLogOn) {
//                LogUtils.log("DCPEncryptor", "decrypt()", "Exception: " + e);
//            }
            return null;
        }

    }

    /**
     * 加密返回数据
     *
     * @param resBody          数据内容
     * @param partnerPublicKey 平台公钥
     * @param selfPrivateKey   商户私钥
     * @return
     */
    public static HashMap<String, Object> encrypt(String resBody, String partnerPublicKey, String selfPrivateKey) {
        try {
//            if (LibDebug.isLogOn) {
//                String test = "{\"amount\":\"0.1\",\"attach\":\"sdfsdf\",\"coinId\":\"10880412808400\",\"goodsTag\":\"冈本\",\"goodsType\":\"冈本2\",\"merchantId\":\"10000000000003\",\"refBizNo\":\"234324234\"}";
//                if (test.equals(resBody)) {
//                    LogUtils.log("DCPEncryptor", "encrypt()", "resBody ok");
//                }
//            }
            HashMap<String, Object> map = new HashMap<>();

            byte[] aesKey = DCPAES.randomKey(DCPAES.DCPAESSize.DCPAES256);
//            if (LibDebug.isLogOn) {
//                aesKey = Base64Comp.decode("kTjgw+od/qaH8wH0XI1rPZ02aKn3YVIFVyOvRRrqcOg=");
//            }
            // 商户公钥 加密key
            byte[] keyEncrypted = DCPRSA.encrypt(aesKey, partnerPublicKey);
            String keyString = Base64Comp.encodeToString(keyEncrypted);
//            if (LibDebug.isLogOn) {
//                String k = "SZ7gGiAPdtXUks7TFIzaUs1HkW8AJ9u3dcH3JF8Iza35PwyeSvZwCLQRBJMBuKXtFfLXnJgLx3/75GKHNcmQoffNAQj56zKWrIjGsP6VMyfavI3vInd4BddGTbIwAZWnNl+bSZfpxQuMZbXPBSGVDBVBcwMDgMndA+vmhKE+duCCP8go2mjGEwg95HRoYcF4+5AJ6Is6Z5oEWwG8+32tlHkzfVqQHhxAvemtM7q38gQLhyCnXpq+b08MQ8Ub0EyqvC13dXQTuX9jA2gtIbkKo7y0FszyLdeobJcOLrWp48xN/xnnJLHuqvVku9y6llBtXzo9LloSVC556NujDpd1IQ==";
//                LogUtils.log("DCPEncryptor", "encrypt()","k.equals: "+k.equals(keyString));
//            }

            //随机iv
            byte[] iv = DCPAES.randomIV();
            //iv = Base64Comp.decode("N2VjZjgzOTBmOTg3MWQ4NQ==");
            String ivString = Base64Comp.encodeToString(iv);

            //加密数据
            byte[] content = DCPAES.encrypt(aesKey, resBody.getBytes(), iv);
            String cipherString = Base64Comp.encodeToString(content);
//            if (LibDebug.isLogOn) {
//                String c = "Bzde8kSGx6URTqc8cQBagL/g8/lQWAtwourPinhXdKT8QWoLCqdO8UNhoq+HXcyHWpKlvSkfkyuIxHemTQNnd9YoAJGCH1wvKb1HTNdTcBHev4jiSQtcTq1Kw/agfICGA7gixdodfCEsElpIrcrZKjbg2HM5XLu915KuKXca0H/YMgqFQbQQ0YZfTH6MF5HCMkUad9aIG/lJgi2IRJwWtg==";
//                LogUtils.log("DCPEncryptor", "encrypt()","c.equals: "+c.equals(cipherString));
//            }

            //平台私钥签名
            String signString = cipherString + keyString + ivString;
            //System.out.println("签名数据:"+signString);
            byte[] signByte = DCPRSA.sign(signString.getBytes("UTF-8"), selfPrivateKey, DCPRSA.DCPRSASignAlgorithm.RSA2);
            String signature = Base64Comp.encodeToString(signByte);
//            if (LibDebug.isLogOn) {
//                String s = "V1CxNPDPBpT6oCQxzuB9CkFSMaXFqv2Nnsdn+FNcb2nZTl7KeBA/Q9+LRI8MYkdUtJVQjp/nvnvKYF/FdXs4SfKPqM9V5W+KpAZgTINKex6RopcRbjeJZ1hCfidk3sqhLTVfdtVovelTZS9MEM707651AzNwII1KO3KXDba9ypjm+1mzm2PKKSaVu/Kykab6oH2k0gd910YssBIvCFKQ483roQGZ2m5hz+LlR8hp/GuABi3jJEB9hk+iAhObOVtyZt14K7mTnkNH7BYL3iBttCspHoKdQ16kcTgP3D1cDSIH6jPyMujIMci9Px/DenLT0Oqe3IfAvfZttZzIunUQ+g==";
//                LogUtils.log("DCPEncryptor", "encrypt()","s.equals: "+s.equals(signature));
//            }
            //
            map.put("cipher", cipherString);
            map.put("iv", ivString);
            map.put("signature", signature);
            map.put("key", keyString);
            return map;
        } catch (Exception e) {
//            if (LibDebug.isLogOn) {
//                LogUtils.log("DCPEncryptor", "encrypt()", "Exception: " + e);
//            }
            return null;
        }
    }

}
