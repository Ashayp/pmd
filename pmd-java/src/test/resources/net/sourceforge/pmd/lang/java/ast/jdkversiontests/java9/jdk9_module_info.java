/*
 * See §7.7 Module Declarations in JLS
 */
open module com.example.foo {
    requires com.example.foo.http;
    requires java.logging;
    requires transitive com.example.foo.network;

    exports com.example.foo.bar;
    exports com.example.foo.internal to com.example.foo.probe;

    uses com.example.foo.spi.Intf;

    provides com.example.foo.spi.Intf with com.example.foo.Impl;
    provides com.example.foo.spi.Intf2 with com.example.foo.Impl, com.example.foo.Impl2;
}
