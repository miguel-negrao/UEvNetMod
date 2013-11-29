UEvNetMod {
    var <desc;
    var <eventNetwork;

    *new { |desc|
        ^super.newCopyArgs(desc)
    }

    connect {
        eventNetwork.actuateNow;
    }

    disconnect {
        eventNetwork.pauseNow;
    }

    asUModFor { |unit|
        eventNetwork = EventNetwork( desc.createDesc(unit) );
        eventNetwork.actuateNow;
    }

    init { }
    start {}
    stop {}
    dispose {
		eventNetwork.pauseNow;
	}
    prepare {}

    storeArgs {
        ^[desc]
    }
}

UEvNetModDef {
    //public
    var <descFunc;

    *new { |descFunc|
        ^super.newCopyArgs( descFunc )
    }

    asUModFor { |unit|
        ^UEvNetMod(this).asUModFor(unit)
    }

    addReactimatesFunc { |unit|
        ^{ |dict|
			Object.checkArgs(UEvNetModDef, "addReactimatesFunc - anonymous function", [dict], [Dictionary]);
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
        ^[descFunc]
    }

}

UEvNetTMod : UEvNetMod {
    var <timer, <tES;

    *new { |def|
        ^super.newCopyArgs(def)
    }

    asUModFor { |unit|
        var tESM;
        timer = ENTimer(desc.delta);
        tESM = timer.asENInput;
        tES = tESM.a;
        eventNetwork = EventNetwork( desc.createDesc(unit, tESM) );
        eventNetwork.actuateNow;
    }

    start { |unit, startPos|
		tES.fire(startPos ? 0);
		timer.start(startPos).unsafePerformIO;
    }

    stop {
        timer.stop.unsafePerformIO;
    }

    dispose {
       timer.stop.unsafePerformIO;
    }

    pause {
        timer.pause.unsafePerformIO;
    }

    resume {
        timer.resume.unsafePerformIO;
    }

	*test { |desc, startTime = 0|
		var tESM, eventNetwork, timer, tES;
        timer = ENTimer(desc.delta);
        tESM = timer.asENInput;
        eventNetwork = EventNetwork(
			tESM >>= { |tEventSource|
				var tSignal = tEventSource.hold(0.0);
				desc.descFunc.(tSignal)
        } );
        eventNetwork.actuateNow;
		timer.start(startTime).unsafePerformIO;
		^eventNetwork
    }

}

UEvNetTModDef : UEvNetModDef {
    var <delta = 0.1;

    *new { |descFunc, delta = 0.1|
        ^super.newCopyArgs(descFunc, delta)
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
        eventNetwork.actuateNow;
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
        ^[timeValuesDict, desc.delta]
    }

}

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