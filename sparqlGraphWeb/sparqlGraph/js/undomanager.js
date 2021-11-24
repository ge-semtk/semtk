/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
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

define([	// properly require.config'ed

			// shimmed
            'sparqlgraph/js/belmont'
         	//'logconfig',
		],

     /*
        Before user operation:
            old_state = current_state

        After user operation:
            if old_state != current_state:
                clear redos stack
                push old state to undo stack

        On undo:
            push current state to redo stack
            set current state to top of undo stack
            pop from undo stack

        On redo:
            push current state to undo stack
            set current state to top of redo stack
            pop from redo stack
    */

	function() {

		var UndoManager = function (setStateCallback) {
            this.setStateCallback = setStateCallback;
			this.reset();
		};

		UndoManager.prototype = {
            reset : function() {
                this.undoStack = [];
                this.redoStack = [];
                this.currentState = null;
            },

            saveState : function (stateJson) {
                state = stateJson ? JSON.stringify(stateJson) : null;
                // save state if it has changed
                if (state != this.currentState) {
                    this.redoStack = [];
                    // don't stack multiple empties in a row
                    if (this.currentState != null || this.undoStack.at(-1) != null) {
                        this.undoStack.push(this.currentState);
                    }
                    this.currentState = state;
                }
            },

			undo : function () {
                // don't stack multiple empties in a row
                if (this.currentState != null || this.redoStack.at(-1) != null) {
                    this.redoStack.push(this.currentState);
                }
                this.currentState = this.undoStack.pop();
                return this.currentState? JSON.parse(this.currentState) : null;
			},

            redo : function () {
                this.undoStack.push(this.currentState);
                // don't stack multiple empties in a row
                if (this.currentState != null || this.redoStack.at(-1) != null) {
                    this.currentState = this.redoStack.pop();
                }
                return this.currentState? JSON.parse(this.currentState) : null;
			},
		};

		return UndoManager;
	}
);
