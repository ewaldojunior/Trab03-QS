/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function getEventX(element, event) {
	var result;
	if (event.offsetX != null) {
		result = event.offsetX;
	} else {
		result = event.pageX;
		do {
			result = result - element.offsetLeft;
			element = element.offsetParent;
		} while (element != null)
	}
	return parseInt(result, 10);
}
function getEventY(element, event) {
	var result;
	if (event.offsetY != null) {
		result = event.offsetY;
	} else {
		result = event.pageY;
		do {
			result = result - element.offsetTop;
			element = element.offsetParent;
		} while (element != null)
	}
	return parseInt(result, 10);
}
function getImage() {
	return Wicket.$("imageContainer").getElementsByTagName("img")[0];
}
function showLoadingIndicator() {
	Wicket.$('loading').style.visibility="visible";
}
function hideLoadingIndicator() {
	Wicket.$('loading').style.visibility="hidden";
}
