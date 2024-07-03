package Parser;

public enum ErrorType {
    IllegalChar, // a
    IdentRedefined, // b
    IdentUndefined, // c
    ParaNumNotMatch, // d
    ParaTypeNotMatch, // e
    ReturnTypeError, // f
    ReturnMissing, // g
    ConstAssign, // h
    SEMICNMissing, // i
    RPARENTMissing, // j
    RBRACKMissing, // k
    PrintfFormatStrNumNotMatch, // l
    BreakContinueNotInLoop, // m
}
