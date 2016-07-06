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


package com.ge.research.semtk.logging;

import java.util.ArrayList;

public class Details {

    private ArrayList<DetailsTuple> detailsTuples;

    public ArrayList<DetailsTuple> asList() {
        return detailsTuples;
    }

    public Details() {
        this.detailsTuples = new ArrayList<>();
    }

    public Details(ArrayList<DetailsTuple> tuples) {
        this.detailsTuples = tuples;
    }

    public Details addDetails(DetailsTuple tuple) {
        detailsTuples.add(tuple);
        return this;
    }

    public Details addDetails(String key, String value) {
        return addDetails(new DetailsTuple(key, value));
    }
}
