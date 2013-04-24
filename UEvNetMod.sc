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
    dispose {}
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
            if( dict.isEmpty ) {
                EventNetwork.returnUnit
            } {
                dict.collect{ |uarg, key|
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

    asUMod { |unit|
        var tESM;
        timer = ENTimer(desc.delta);
        tESM = timer.asENInput;
        tES = tESM.a;
        eventNetwork = EventNetwork( desc.createDesc(unit, tESM) );
        eventNetwork.actuateNow;
    }

    start {
        tES.fire(0);
        timer.start.unsafePerformIO;
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

}

UEvNetTModDef : UEvNetModDef {
    var <delta = 0.1;

    *new { |descFunc, delta = 0.1|
        ^super.newCopyArgs(descFunc, delta)
    }

    asUMod { |unit|
        ^UEvNetTMod(this).asUMod(unit, delta)
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