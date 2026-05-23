package com.amagari.translationtool.client.paratranz;

import java.io.IOException;

public class ParaTranzApiException extends IOException {
	private final int statusCode;

	public ParaTranzApiException(int statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
	}

	public int statusCode() {
		return statusCode;
	}

	public boolean permissionDenied() {
		return statusCode == 403;
	}
}
