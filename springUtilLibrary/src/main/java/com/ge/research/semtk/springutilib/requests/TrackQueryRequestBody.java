/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */


package com.ge.research.semtk.springutilib.requests;

import io.swagger.v3.oas.annotations.media.Schema;

public class TrackQueryRequestBody extends SparqlEndpointOptionalRequestBody {
	
	@Schema(
			name = "key",
			required = false,
			example = "uuid-1234-abcd-1234")
	public String key = null;

	@Schema(
			name = "user",
			required = false,
			example = "fred")
	public String user = null;

	@Schema(
			name = "startEpoch",
			required = false,
			example = "1599164758")
	public Long startEpoch = null;
	
	@Schema(
			name = "endEpoch",
			required = false,
			example = "1599164759")
	public Long endEpoch = null;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Long getStartEpoch() {
		return startEpoch;
	}

	public void setStartEpoch(Long startEpoch) {
		this.startEpoch = startEpoch;
	}

	public Long getEndEpoch() {
		return endEpoch;
	}

	public void setEndEpoch(Long endEpoch) {
		this.endEpoch = endEpoch;
	}
	
}
