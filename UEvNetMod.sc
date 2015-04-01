UEvNetMod {
	classvar <>globalkeySignalDict;
	classvar <allEventNetworks;
	var <defName;
	//private
	var <def;
	var <>keySignalDict;
	var sliderValues;
	var <sliderProxys;
	var <unit;

	*initClass {
		allEventNetworks = IdentityDictionary.new
	}

	*new { |defName, sliderValues|
		var def = UEvNetModDef.all[defName];
		if(def.notNil) {
			^super.new.initUEvNetMod(defName, def, sliderValues.asCollection/*converts nil to []*/)
		}{
			Error("No UEvNetModDef with name %".format(defName)).throw
		}
	}

	initUEvNetMod { |adefName, adef, initSliderValues|
		var initSliderValuesDict, sliderValues2;
		defName = adefName;
		def = adef;
		initSliderValuesDict = initSliderValues.clump(2).collect{ |xs| xs[0].asSymbol -> xs[1] }.asIdentDictFromAssocs;
		sliderValues2 = def.sliderSpecs.clump(2).collect{ |xs|
			var key = xs[0];
			var val = initSliderValuesDict.at(key);
			if( val.isNil) { xs[1].default }{ val }
		};
		sliderValues = sliderValues2;
	}

	eventNetwork {
		^allEventNetworks.at(this)
	}

	connect {
		this.eventNetwork.start;
	}

	disconnect {
		this.eventNetwork.stop;
	}

	asUModFor { |aunit|
		var en;
		unit = aunit;
		sliderProxys = sliderValues.collect{ |v|  FRPGUIProxy(nil, v) };
		en = EventNetwork( def.createDesc(unit, sliderProxys.collect(_.asENInput) ) );
		en.start;
		allEventNetworks.put( this, en);
		keySignalDict = UEvNetMod.globalkeySignalDict;
	}

	//need diferent method name from generic UMod, to distinguish which umods need to regenerate after deepcopy
	regenerate { |unit|
		sliderValues = this.sliderValues;
		this.asUModFor(unit)
	}

	viewNumLines{ ^sliderValues.size+2 }

	init { }
	start {}
	stop {}
	//dispose happens when synths are freed.
	//we don't want to free event network at that point
	dispose {}
	prepare {}

	sliderValues {
		^if(sliderProxys.isNil){
			sliderValues
		} {
			sliderProxys.collect( _.value )
		}
	}

	sliderValuesKeyPairs {
		^[this.def.sliderKeys, this.sliderValues].flop.flatten
	}

	storeArgs {
		^if(this.def.sliderSpecs.size == 0) {
			[defName]
		} {
			[defName, this.sliderValuesKeyPairs]
		}
	}

	newFromStoreArgs {
		^this.class.new(*this.storeArgs)
	}

	withNewDef { |newDefName|
		^this.class.new(newDefName, this.sliderValuesKeyPairs)
	}

	def_ { |newDefName|
		var en;
		var def = UEvNetModDef.all[newDefName];
		if(def.notNil) {
			//"def_ unit: %".format(unit).postln;
			this.eventNetwork.stop;
			this.initUEvNetMod(newDefName, def, this.sliderValuesKeyPairs );
			sliderProxys = sliderValues.collect{ |v|  FRPGUIProxy(nil, v) };
			en = EventNetwork( def.createDesc(unit, sliderProxys.collect(_.asENInput) ) );
			en.start;
			allEventNetworks.put( this, en);
			keySignalDict = UEvNetMod.globalkeySignalDict;
			this.changed(\def);
		}{
			Error("No UEvNetModDef with name %".format(newDefName)).throw
		}
	}

	makeView { |parent, bounds, unit, redrawUChainGUIAction|
		var viewHeight = 14;

		StaticText(parent, (bounds.width-30-4-12-4)@viewHeight)
		.applySkin( RoundView.skin )
		.string_("mod : "++this.defName);
		SmoothButton(parent, 30@viewHeight )
		.border_( 1 )
		.radius_( 2 )
		.states_([["edit"]])
		.action_{
			var text = TextView().string_(this.def.cs).syntaxColorize;
			var bt = Button().states_([["save"]]).action_{
				/*var oldMod = unit.mod;
				var newMod;
				var t = if( oldMod.isKindOf(UEvNetTMod) and: { oldMod.playing } ) {
					Some( oldMod.timer.t )
				} { None() };
				text.string.interpret;
				newMod = oldMod.newFromStoreArgs;
				unit.mod_( newMod );
				t.do{ |t|
					newMod.start(nil, t)
				};
				*/
				this.def_(text.string.interpret.name);
				redrawUChainGUIAction.();
			};
			var w = Window("edit mod def "++this.defName).layout_(VLayout(text,bt)).front;
			redrawUChainGUIAction.();
		};
		SmoothButton( parent, 12@12 )
		.label_( '-' )
		.border_( 1 )
		.resize_(3)
		.action_({
			unit.mod_(nil);
			redrawUChainGUIAction.();
		});
		if( def.sliderSpecs.size > 0) {
			var specspairs = def.sliderSpecs.clump(2).flop;
			var labels = specspairs[0];
			var specs = specspairs[1];

			[this.sliderValues, specs, labels, sliderProxys].flopWith{ |v, spec, label, proxy|
				var bounds2 = (bounds.width @ ((spec.viewNumLines * viewHeight) + ((spec.viewNumLines-1) * 4)));
				var composite = CompositeView( parent, bounds2 )
				.resize_(2)
				.background_(Color.grey(0.7));
				var viewDict = spec.makeView( composite, bounds2, "*"++label,
					{ |vw, value| }, 5
				);
				viewDict[ \valueView ].value = v;
				proxy.view_(viewDict[ \valueView ])
			}
		}
	}

}

UEvNetModDef {
	classvar <>all;
	//public
	var <name, <descFunc;
	var <sliderSpecs;
	var <>category;

	*new { |name, descFunc, sliderSpecs=#[]|
		var check1 = if(sliderSpecs.size.odd){ Error("ImmDef - sliderSpecs: array size must be even").throw };
		var res = super.new.initUEvNetModDef( name, descFunc, sliderSpecs );
		res.addToAll;
		^res
	}

	initUEvNetModDef { | aname, adescFunc, asliderSpecs |
		name = aname;
		descFunc = adescFunc;
		sliderSpecs = asliderSpecs.clump(2).collect{ |xs| [xs[0].asSymbol, xs[1].asControlSpec] }.flatten;
		category = \default;
	}

	addToAll {
		UEvNetModDef.all ?? { UEvNetModDef.all = IdentityDictionary() };
		UEvNetModDef.all[ name.asSymbol ] = this;
		UEvNetModDef.all.changed( \added, this );
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

	createDesc { |unit, slidersM|
		^slidersM.sequence(EventNetwork) >>= { |sliderSigs|
			descFunc.(*sliderSigs) >>= this.addReactimatesFunc(unit)
		}
	}

	sliderKeys {
		^sliderSpecs.clump(2).flop[0]
	}

	storeArgs {
		^[name, descFunc] ++ if(sliderSpecs.size == 0){[]}{[sliderSpecs]}
	}

}

UEvNetTMod : UEvNetMod {

	var <timer, <tES;
	var <playing  = false;

	asUModFor { |aunit|
		var en;
		var tESM;
		unit = aunit;
		timer = ENTimer(def.delta);
		tESM = timer.asENInput;
		tES = tESM.a;
		sliderProxys = sliderValues.collect{ |v|  FRPGUIProxy(nil, v) };
		en = EventNetwork( def.createDesc(unit, tESM, sliderProxys.collect(_.asENInput)) );
		en.start;
		allEventNetworks.put( this, en );
		keySignalDict = UEvNetMod.globalkeySignalDict;
	}

	start { |unit, startPos|
		if(playing.not){
			tES.fire(startPos ? 0);
			timer.start(startPos).unsafePerformIO;
			playing = true
		}
	}

	stop {
		//"UEvNetTMod stop".postln;
		if(playing){
			timer.stop.unsafePerformIO;
			playing = false
		}
	}

	disconnect {
		this.stop;
		this.eventNetwork.stop
	}

	dispose {
		this.stop
	}

	pause {
		timer.pause.unsafePerformIO;
		playing = false;
	}

	resume {
		timer.resume.unsafePerformIO;
		playing = true;
	}

	def_ { |newDefName|
		var en;
		var def = UEvNetModDef.all[newDefName];
		if(def.notNil) {
			var tESM;
			this.eventNetwork.stop;
			this.initUEvNetMod(newDefName, def, this.sliderValuesKeyPairs );
			tESM = timer.asENInput;
			tES = tESM.a;
			sliderProxys = sliderValues.collect{ |v|  FRPGUIProxy(nil, v) };
			en = EventNetwork( def.createDesc(unit, tESM, sliderProxys.collect(_.asENInput) ) );
			en.start;
			allEventNetworks.put( this, en);
			keySignalDict = UEvNetMod.globalkeySignalDict;
			this.changed(\def);
		}{
			Error("No UEvNetModDef with name %".format(newDefName)).throw
		}
	}


	/*
	//untested
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
	*/

}

UEvNetTModDef : UEvNetModDef {
	var <delta = 0.1;

	*new { |defName, descFunc, delta = 0.1, sliderSpecs=#[]|
		var x = super.new(defName, descFunc, sliderSpecs).initUEvNetTModDef(delta);
		x.addToAll;
		^x
	}

	initUEvNetTModDef { |adelta|
		delta = adelta
	}

	/*
	untested
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
	*/
	asUModFor { |unit|
		^UEvNetTMod(this).asUModFor(unit, delta)
	}

	createDesc { |unit, tESM, slidersM|
		^tESM >>= { |tEventSource|
			var tSignal = tEventSource.hold(0.0);
			slidersM.sequence(EventNetwork) >>= { |sliderSigs|
				descFunc.(*([tSignal]++sliderSigs) )
				>>= this.addReactimatesFunc(unit, tEventSource)
			}
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
		^[name, descFunc, delta]  ++ if(sliderSpecs.size == 0){[]}{[sliderSpecs]}
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

	createDesc { |unit, slidersM|
		^slidersM.sequence(EventNetwork) >>= { |sliderSigs|
			ENDef.evaluate( descFunc, sliderSigs ) >>= this.addReactimatesFunc(unit)
		}
	}

}

UENTModDef : UEvNetTModDef {

	createDesc { |unit, tESM, slidersM|
		^tESM >>= { |tEventSource|
			var tSignal = tEventSource.hold(0.0);
			slidersM.sequence(EventNetwork) >>= { |sliderSigs|
				ENDef.evaluate( descFunc, [tSignal]++sliderSigs)
				>>= this.addReactimatesFunc(unit, tEventSource)
			}
		}
	}
}

+ U {

	updateModFromDef {
		var oldMod = this.mod;
		var newMod;
		var t = if( oldMod.isKindOf(UEvNetTMod) and: { oldMod.playing } ) {
			Some( oldMod.timer.t )
		} { None() };
		newMod = oldMod.newFromStoreArgs;
		this.mod_( newMod );
		t.do{ |t|
			newMod.start(nil, t)
		};
	}

}