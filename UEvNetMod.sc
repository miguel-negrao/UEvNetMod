UEvNetMod {
	classvar <>globalkeySignalDict;
    var <defName;
	//private
	var <def;
    var <eventNetwork;
	var <>keySignalDict;

    *new { |defName|
		var def = UEvNetModDef.all[defName];
		if(def.notNil) {
			^super.newCopyArgs(defName, def)
		}{
			Error("No UEvNetModDef with name %".format(defName)).throw
		}
    }

    connect {
        eventNetwork.start;
    }

    disconnect {
        eventNetwork.pauseNow;
    }

    asUModFor { |unit|
        eventNetwork = EventNetwork( def.createDesc(unit) );
        eventNetwork.start;
		keySignalDict = UEvNetMod.globalkeySignalDict;
    }

	viewNumLines{ ^0 }

    init { }
    start {}
    stop {}
	//dispose happens when synths are freed.
	//we don't want to free event network at that point
    dispose {}
    prepare {}

    storeArgs {
        ^[defName]
    }
}

UEvNetModDef {
	classvar <>all;
    //public
    var <name, <descFunc;

    *new { |name, descFunc|
        var x = super.newCopyArgs( name, descFunc );
		x.addToAll;
		^x
    }

	addToAll {
		UEvNetModDef.all ?? { UEvNetModDef.all = IdentityDictionary() };
		UEvNetModDef.all[ name.asSymbol ] = this;
		UEvNetModDef.all.changed( \added, this );
	}

    asUModFor { |unit|
        ^UEvNetMod(name).asUModFor(unit)
    }

    addReactimatesFunc { |unit|
        ^{ |dict|
			Object.checkArgs(UEvNetModDef, "addReactimatesFunc - anonymous function", [dict], [Dictionary]);
			//unpure naughtiness, might be usefull later
			UEvNetMod.globalkeySignalDict = dict;
            if( dict.isEmpty ) {
                EventNetwork.returnUnit
            } {
				var unitKeys = unit.def.argSpecs.collect(_.name);
                dict.collect{ |uarg, key|
					Object.checkArgs(UEvNetModDef, \addReactimatesFunc, [uarg,key], [UModArg, Symbol]);
					if( unitKeys.includes(key).not ) {
						"WARNING: unit % doesn't have key %".format(unit.name, key).postln;
					};
                    uarg.match({ |sig|
                        sig.collect{ |v| IO{ unit.mapSet(key, v) } }.reactimate
                        },{ |sig|
                            sig.collect{ |v| IO{ unit.set(key, v ) } }.reactimate
                    })
                }.as(Array).reduce('>>=|')
            }
        }
    }

    createDesc { |unit|
        ^descFunc.() >>= this.addReactimatesFunc(unit)
    }

    storeArgs {
        ^[name, descFunc]
    }

}

UEvNetTMod : UEvNetMod {
    var <timer, <tES;
	var <playing  = false;

    asUModFor { |unit|
        var tESM;
        timer = ENTimer(def.delta);
        tESM = timer.asENInput;
        tES = tESM.a;
        eventNetwork = EventNetwork( def.createDesc(unit, tESM) );
        eventNetwork.start;
		keySignalDict = UEvNetMod.globalkeySignalDict;
    }

    start { |unit, startPos|
		tES.fire(startPos ? 0);
		timer.start(startPos).unsafePerformIO;
		playing = true;
    }

    stop {
        timer.stop.unsafePerformIO;
		playing = false;
    }

	disconnect {
       this.stop;
		eventNetwork.stop
    }

    pause {
        timer.pause.unsafePerformIO;
		playing = false;
    }

    resume {
        timer.resume.unsafePerformIO;
		playing = true;
    }

	*test { |def, startTime = 0|
		var tESM, eventNetwork, timer, tES;
        timer = ENTimer(def.delta);
        tESM = timer.asENInput;
        eventNetwork = EventNetwork(
			tESM >>= { |tEventSource|
				var tSignal = tEventSource.hold(0.0);
				def.descFunc.(tSignal)
        } );
        eventNetwork.start;
		timer.start(startTime).unsafePerformIO;
		^eventNetwork
    }

}

UEvNetTModDef : UEvNetModDef {
    var <delta = 0.1;

    *new { |defName, descFunc, delta = 0.1|
        var x = super.newCopyArgs(defName, descFunc, delta);
		x.addToAll;
		^x
    }

	test{ |startTime = 0|
		var tESM, eventNetwork, timer;
        timer = ENTimer(delta);
        tESM = timer.asENInput;
        eventNetwork = EventNetwork(
			tESM >>= { |tEventSource|
				var tSignal = tEventSource.hold(0.0);
				descFunc.(tSignal)
        } );
        eventNetwork.start;
		timer.start(startTime).unsafePerformIO;
		^eventNetwork
	}

    asUModFor { |unit|
        ^UEvNetTMod(this).asUModFor(unit, delta)
    }

    createDesc { |unit, tESM|
        ^tESM >>= { |tEventSource|
            var tSignal = tEventSource.hold(0.0);
            descFunc.(tSignal)
            >>= this.addReactimatesFunc(unit, tEventSource)
        }
    }

    addReactimatesFunc { |unit, tEventSource|
        ^{ |dict|
			UEvNetMod.globalkeySignalDict = dict;
			if( dict.isEmpty ) {
                EventNetwork.returnUnit
            } {
                dict.collect{ |uarg, key|
                    uarg.match({ |sig|
                        (sig <@ tEventSource).collect{ |v| IO{ unit.mapSet(key, v) } }.reactimate
                        },{ |sig|
                            (sig <@ tEventSource).collect{ |v| IO{ unit.set(key, v ) } }.reactimate
                    })
                }.as(Array).reduce('>>=|')
            }
        }
    }

    storeArgs {
        ^[descFunc, delta]
    }
}

//language side automation
/*
UAutMod : UEvNetTMod {
	var <timeValuesDict;
    /*
    timeValuesDict ( \freq: [ [0.0, 1.0], [0.1, 3.0], ... ], \amp: [...] )
    */

    *new{ |timeValuesDict, delta = 0.1|
        var descFunc = { |t|
            EventNetwork.returnDesc( timeValuesDict.collect{ |timesValues, key|
                var times, values, interpolateFunc, sig;
                #times, values = timesValues.flop;
                //"values: %".format(values).postln;
                //"times: %".format(times).postln;
                interpolateFunc = { |t|
                    values.blendAt( times.indexInBetween(t) )
                };
                sig = interpolateFunc <%> t.changes.select({ |t| t < times.last }).hold(0.0);
                //sig.do{ |x| "key % : %".format(key,x).postln };
                UArg( sig )
            } )
        };
        ^super.new( UEvNetTModDef( descFunc, delta) ).initUAutMod( timeValuesDict )
	}

	initUAutMod { |inTimeValuesDict|
        timeValuesDict = inTimeValuesDict;
	}

	storeArgs {
        ^[timeValuesDict, def.delta]
    }

}
*/
/*
UENMods

Allow using an imperative interface to the EventNetwork monad.

*/
UENModDef : UEvNetModDef {

	 createDesc { |unit|
		^ENDef.evaluate( descFunc ) >>= this.addReactimatesFunc(unit)
    }

}

UENTModDef : UEvNetTModDef {

    createDesc { |unit, tESM|
        ^tESM >>= { |tEventSource|
            var tSignal = tEventSource.hold(0.0);
			ENDef.evaluate( descFunc, [tSignal] )
            >>= this.addReactimatesFunc(unit, tEventSource)
        }
    }
}