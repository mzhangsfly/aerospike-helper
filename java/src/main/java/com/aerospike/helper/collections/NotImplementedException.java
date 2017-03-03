/* Copyright 2012-2017 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.aerospike.helper.collections;

public class NotImplementedException extends RuntimeException {

	/**
	 * This exception indicates the the method is not implemented.
	 */
	private static final long serialVersionUID = 6989807145209111662L;

	public NotImplementedException() {
		super("Method not implemented");
	}

	public NotImplementedException(String message, Throwable cause, boolean enableSuppression,
								   boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotImplementedException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotImplementedException(String message) {
		super(message);
	}

	public NotImplementedException(Throwable cause) {
		super(cause);
	}

}
