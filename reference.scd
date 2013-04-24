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

y.asCompileString


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

y.asCompileString

(
x = UAutMod( (\freq: [[0.0,400], [5.0, 1000], [7.0, 200] ] , \amp: [[0.0,0.5], [30.0, 0.0]] ) );
y = UChain( [\sine, [\freq, 400], x], \stereoOutput);

y.gui;
y.start
)

y.asCompileString

y.units[0].mod.insp