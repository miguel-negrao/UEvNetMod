
+ U {
	//signal with [key1, value1, key2, value2]
	setSink { |signal|
		^signal.collect{ |args| IO{ this.set(*args) } }.reactimate
	}

	//signal with [key1, value1, key2, value2]
	mapSetSink { |signal|
		^signal.collect{ |args| IO{ this.mapSet(*args) } }.reactimate
	}

	enSetSink { |signal|
		^ENDef.appendToResult( this.setSink(signal) );
	}

	//signal with [key1, value1, key2, value2]
	enMapSetSink { |signal|
		^ENDef.appendToResult( this.mapSetSink(signal) );
	}

}


		