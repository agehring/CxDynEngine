/*******************************************************************************
 * Copyright (c) 2017-2019 Checkmarx
 *  
 * This software is licensed for customer's internal use only.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package com.checkmarx.engine.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {
	
	private Integer messageCode;
	private String messageDetails;
	private String message;
	
	public ErrorResponse() {
		// default .ctor for unmarshalling
	}
	
	public ErrorResponse(Integer messageCode, String messageDetails) {
		this.messageCode = messageCode;
		this.messageDetails = messageDetails;
	}

	public String getMessage() {
		return message;
	}

	public Integer getMessageCode() {
		return messageCode;
	}

	public String getMessageDetails() {
		return messageDetails;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("message", message)
				.add("messageCode", messageCode)
				.add("messageDetails", messageDetails)
				.omitNullValues()
				.toString();
	}

}
