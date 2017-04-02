package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import org.apache.commons.codec.digest.DigestUtils;

import edu.illinois.uiuc.sp17.cs425.team4.component.HashFunction;

public class SHA1HashFunction implements HashFunction {

	@Override
	public byte[] hash(byte[] obj) {
		return DigestUtils.sha1(obj);
	}

}
