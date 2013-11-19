(

f = {
	var w = Window.new.front;
	var sl1 = Slider().orientation_(\horizontal);
	var sl2 = Slider().orientation_(\horizontal);
	w.layout_( VLayout( sl1, sl2 ) );
	Do(
		slEv1 <- sl1.asENInput;
		slEv2 <- sl2.asENInput;
		slEv1.debug("sl");
		return ( freq: USpecArg( {|a,b| a.mod(b) } <%> slEv1 <*> slEv2) )
	)
};
x = UEvNetModDef(f);
y = UChain( [\sine, [\freq, 400], x], \stereoOutput);

y.gui;
y.start
)
(

var w = Window.new.front;
var sl1 = Slider().orientation_(\horizontal);
var sl2 = Slider().orientation_(\horizontal);
w.layout_( VLayout( sl1, sl2 ) );


f = {

	var slEv1 = sl1.enIn; //signal from slider 1
	var slEv2 = sl2.enIn; //signal from slider 2

	var x = {|a,b| a.mod(b) }.lift.(slEv1, slEv2); //binary op on two signals

	//return dictionary describing signals to be connected to synth controls
	//USpecArg means that x is in [0,1] range and will be automatically mapped to the freq range
	(freq: USpecArg( x ) )
};

//analogous to synthdef
x = UENModDef(f);

//this is a like a group of synths with automatic bus management
y = UChain( [\sine, [\freq, 400], x], \stereoOutput);

y.gui;
y.start
)

y.asCompileString
Quarks.gui

(

f = {
	Do(
		slEv1 <- MIDIMKtl('nnkn0').signalFor(\sl_1_1).asENInput;
		slEv2 <- MIDIMKtl('nnkn0').signalFor(\sl_2_1).asENInput;
		return ( freq: USpecArg(slEv1) , amp: USpecArg(slEv2) )
	)
};
x = UEvNetModDef(f);
y = UChain( [\sine, [\freq, 400], x], \stereoOutput);

y.gui;
y.start
)

y.asCompileString


(
f = { |t|
	var freq = t.collect{ |t| sin(2*pi*t*0.1)  }.linlin(-1.0,1.0, 400, 800);
	Do(
		freq.debug("freq");
		return ( freq: UArg(freq) )
	)
};
x = UEvNetTModDef(f, 0.1);
y = UChain( [\sine, [\freq, 400], x], \stereoOutput);

y.gui;
y.start
)

//Testing side effects:
(
f = { |t|
	var x = t.postln;
	var freq = t.collect{ |t| sin(2*pi*t*0.1)  }.linlin(-1.0,1.0, 400, 800);
	Do(
		freq.debug("freq");
		return ( freq: UArg(freq) )
	)
};
x = UEvNetTMod.test( UEvNetTModDef(f, 0.1) )
);

//with score, check advancing start time.
(
f = { |t|
	var freq = t.collect{ |t| sin(2*pi*t*0.1)  }.linlin(-1.0,1.0, 400, 800);
	Do(
		freq.debug("freq");
		return ( freq: UArg(freq) )
	)
};
x = UEvNetTModDef(f, 0.1);
y = UChain(0,0,60, [\sine, [\freq, 400], x], \stereoOutput);
z = UScore(y);
z.gui;

)

(
f = { |t|
	var freq = t.linlin(0.0, 60.0, 400, 800);
	Do(
		t.debug("t");
		freq.debug("freq");
		return ( freq: UArg(freq) )
	)
};
x = UEvNetTModDef(f, 0.1);
y = UChain(0,0,60, [\sine, [\freq, 400], x], \stereoOutput);
z = UScore(y);
z.gui;

)




y.asCompileString

(
x = UAutMod( (\freq: [[0.0,400], [5.0, 1000], [7.0, 200] ] , \amp: [[0.0,0.5], [30.0, 0.0]] ) );
y = UChain( [\sine, [\freq, 400], x], \stereoOutput);

y.gui;
y.start
)

y.asCompileString

y.units[0].mod.insp\

(
w = Window.new.front;
~sl1 = Slider().orientation_(\horizontal);
~sl2 = Slider().orientation_(\horizontal);
w.layout_( VLayout( ~sl1, ~sl2 ) );
~proxy1.view_(~sl1);
~proxy2.view_(~sl2);
)

~proxy1.removeView
~proxy2.removeView

(
~proxy1 = FRPGUIProxy();
~proxy2 = FRPGUIProxy();
f = {

	Do(
		slEv1 <- ~proxy1.asENInput;
		slEv2 <- ~proxy2.asENInput;
		slEv1.debug("sl");
		return ( freq: USpecArg( {|a,b| a.mod(b) } <%> slEv1 <*> slEv2) )
	)
};
x = UEvNetModDef(f);
y = UChain( [\sine, [\freq, 400], x], \stereoOutput);

y.gui;
y.start
)