(
f = {
	var w = Window.new.front;
	var sl1 = Slider().orientation_(\horizontal);
	var sl2 = Slider().orientation_(\horizontal);
	var slEv1 = sl1.enIn;
    var slEv2 = sl2.enIn;

	w.layout_( VLayout( sl1, sl2 ) );
	slEv1.debug("sl");

	( freq: USpecArg( {|a,b| a.mod(b) } <%> slEv1 <*> slEv2) )
};

y = UChain( [\sine, [\freq, 400], UENModDef(f)], \stereoOutput);

y.gui;
y.start
)

y.asCompileString


(

f = {

	var slEv1 = MIDIMKtl('nnkn0').signalFor(\sl_1_1).enIn;
	var slEv2 = MIDIMKtl('nnkn0').signalFor(\sl_2_1).enIn;

	( freq: USpecArg(slEv1) , amp: USpecArg(slEv2) )
};
x = UENModDef(f);
y = UChain( [\sine, [\freq, 400], x], \stereoOutput);

y.gui;
y.start
)

y.asCompileString


(
f = { |t|

    var freq = t.collect{ |t| sin(2*pi*t*0.1)  }.linlin(-1.0,1.0, 400, 800);

	freq.debug("freq");

	( freq: UArg(freq) )
};
x = UENTModDef(f, 0.1);
y = UChain( [\sine, [\freq, 400, \lag, 0.1], x], \stereoOutput);

y.gui;
y.start
)

y.asCompileString