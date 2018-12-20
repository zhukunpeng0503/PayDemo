package com.zkp.walletdemo;
/** 
 * @author 作者 E-mail: panhang@ehoo.cn
 * @version 创建时间：Dec 29, 2014 12:51:47 PM 
 */

public interface Encryptor {
	/**
	 * 数据加密接口
	 * 
	 * @param org
	 *            需要加密的原始数据
	 * @return 加密后的数据
	 */
	public String encrypt(String org);

	/**
	 * 数据解密接口
	 * 
	 * @param ed
	 *            需要解密的数据
	 * @return 解密后的数据
	 */
	public String decrypt(String ed);
}
