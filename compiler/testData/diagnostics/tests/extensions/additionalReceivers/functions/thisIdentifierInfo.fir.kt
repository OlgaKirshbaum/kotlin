class A(val a: String?)

with<A> fun f() {
    if (<!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!> == null) return
    <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>length<!>
}