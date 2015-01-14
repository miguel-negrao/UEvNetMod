UModArg {
    var <signal;

    printOn { arg stream;
		stream << this.class.name << "( " << signal  << " )"
	}

	collect { |f|
		^this.class.new( f.(signal) )
	}
}

UArg : UModArg {
    *new { |signal| ^super.newCopyArgs(signal) }
    match { |fspec, fnospec|
        ^fnospec.(signal);
    }
}

USpecArg : UModArg {
    *new { |signal| ^super.newCopyArgs(signal) }
    match { |fspec, fnospec|
        ^fspec.(signal);
    }
}