UModArg {
    var <signal;

    printOn { arg stream;
		stream << this.class.name << "( " << signal  << " )"
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