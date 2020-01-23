/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.meta

sealed class Node {

    /**
     * Author didn't forget about the existence of reflection.
     * The use of reflection in stdlib is prohibited, so the author resorts
     * to such terrible methods. =(
     */

    companion object {
        const val PREFIX = "kotlin.meta.Node."
    }

    protected enum class ArgType { VALUE, LIST }
    protected data class ArgDesc(val name: String, val value: String, val type: ArgType)

    protected fun stringifyValue(s: String?) = if (s == null) s.toString() else "\"" + s + "\""
    protected fun stringifyList(l: List<String?>) = l.map { "\"" + l.toString() + "\"" }.toString()

    protected fun stringRepresentation(className: String, args: List<ArgDesc>): String {
        fun arrayToListOf(arrayString: String) = "listOf(${arrayString.substring(1, arrayString.length - 1)})"
        return "$PREFIX$className(${
        args.joinToString(", ") {
            when (it.type) {
                ArgType.VALUE -> "${it.name}=${it.value}"
                ArgType.LIST  -> "${it.name}=${arrayToListOf(it.value)}"
            }
        }
        })"
    }

    var tag: Any? = null

    interface WithAnnotations {
        val anns: List<Modifier.AnnotationSet>
    }

    interface WithModifiers : WithAnnotations {
        val mods: List<Modifier>
        override val anns: List<Modifier.AnnotationSet> get() = mods.mapNotNull { it as? Modifier.AnnotationSet }
    }

    interface Entry : WithAnnotations {
        val pkg: Package?
        val imports: List<Import>
    }

    data class File(
        override val anns: List<Modifier.AnnotationSet>,
        override val pkg: Package?,
        override val imports: List<Import>,
        val decls: List<Decl>
    ) : Node(), Entry {
        override fun toString() = stringRepresentation(
            "File", listOf(
                ArgDesc("anns", anns.toString(), ArgType.LIST),
                ArgDesc("pkg", pkg.toString(), ArgType.VALUE),
                ArgDesc("imports", imports.toString(), ArgType.LIST),
                ArgDesc("decls", decls.toString(), ArgType.LIST)
            )
        )
    }

    data class Script(
        override val anns: List<Modifier.AnnotationSet>,
        override val pkg: Package?,
        override val imports: List<Import>,
        val exprs: List<Expr>
    ) : Node(), Entry {
        override fun toString() = stringRepresentation(
            "Script", listOf(
                ArgDesc("anns", anns.toString(), ArgType.LIST),
                ArgDesc("pkg", pkg.toString(), ArgType.VALUE),
                ArgDesc("imports", imports.toString(), ArgType.LIST),
                ArgDesc("exprs", exprs.toString(), ArgType.LIST)
            )
        )
    }

    data class Package(
        override val mods: List<Modifier>,
        val names: List<String>
    ) : Node(), WithModifiers {
        override fun toString() = stringRepresentation(
            "Package", listOf(
                ArgDesc("mods", mods.toString(), ArgType.LIST),
                ArgDesc("names", stringifyList(names), ArgType.LIST)
            )
        )
    }

    data class Import(
        val names: List<String>,
        val wildcard: Boolean,
        val alias: String?
    ) : Node() {
        override fun toString() = stringRepresentation(
            "Import", listOf(
                ArgDesc("names", stringifyList(names), ArgType.LIST),
                ArgDesc("wildcard", wildcard.toString(), ArgType.VALUE),
                ArgDesc("alias", stringifyValue(alias), ArgType.VALUE)
            )
        )
    }

    sealed class Decl : Node() {
        data class Structured(
            override val mods: List<Modifier>,
            val form: Form,
            val name: String,
            val typeParams: List<TypeParam>,
            val primaryConstructor: PrimaryConstructor?,
            val parentAnns: List<Modifier.AnnotationSet>,
            val parents: List<Parent>,
            val typeConstraints: List<TypeConstraint>,
            // TODO: Can include primary constructor
            val members: List<Decl>
        ) : Decl(), WithModifiers {
            override fun toString() = stringRepresentation(
                "Decl.Structured", listOf(
                    ArgDesc("mods", mods.toString(), ArgType.LIST),
                    ArgDesc("form", form.toString(), ArgType.VALUE),
                    ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                    ArgDesc("typeParams", typeParams.toString(), ArgType.LIST),
                    ArgDesc("primaryConstructor", primaryConstructor.toString(), ArgType.VALUE),
                    ArgDesc("parentAnns", parentAnns.toString(), ArgType.LIST),
                    ArgDesc("parents", parents.toString(), ArgType.LIST),
                    ArgDesc("typeConstraints", typeConstraints.toString(), ArgType.LIST),
                    ArgDesc("members", members.toString(), ArgType.LIST)
                )
            )

            enum class Form {
                CLASS, ENUM_CLASS, INTERFACE, OBJECT, COMPANION_OBJECT;

                override fun toString() = PREFIX + "Decl.Structured.Form." + super.toString()
            }

            sealed class Parent : Node() {
                data class CallConstructor(
                    val type: TypeRef.Simple,
                    val typeArgs: List<Node.Type?>,
                    val args: List<ValueArg>,
                    val lambda: Expr.Call.TrailLambda?
                ) : Parent() {
                    override fun toString() = stringRepresentation(
                        "Decl.Structured.Parent.CallConstructor", listOf(
                            ArgDesc("type", type.toString(), ArgType.VALUE),
                            ArgDesc("typeArgs", typeArgs.toString(), ArgType.LIST),
                            ArgDesc("args", args.toString(), ArgType.LIST),
                            ArgDesc("lambda", lambda.toString(), ArgType.VALUE)
                        )
                    )
                }

                data class Type(
                    val type: TypeRef.Simple,
                    val by: Expr?
                ) : Parent() {
                    override fun toString() = stringRepresentation(
                        "Decl.Structured.Parent.Type", listOf(
                            ArgDesc("type", type.toString(), ArgType.VALUE),
                            ArgDesc("by", by.toString(), ArgType.VALUE)
                        )
                    )
                }
            }

            data class PrimaryConstructor(
                override val mods: List<Modifier>,
                val params: List<Func.Param>
            ) : Node(), WithModifiers {
                override fun toString() = stringRepresentation(
                    "Decl.Structured.PrimaryConstructor", listOf(
                        ArgDesc("mods", mods.toString(), ArgType.LIST),
                        ArgDesc("params", params.toString(), ArgType.LIST)
                    )
                )
            }
        }

        data class Init(val block: Block) : Decl() {
            override fun toString() = stringRepresentation(
                "Decl.Init", listOf(
                    ArgDesc("block", block.toString(), ArgType.VALUE)
                )
            )
        }

        data class Func(
            override val mods: List<Modifier>,
            val typeParams: List<TypeParam>,
            val receiverType: Type?,
            // Name not present on anonymous functions
            val name: String?,
            val paramTypeParams: List<TypeParam>,
            val params: List<Param>,
            val type: Type?,
            val typeConstraints: List<TypeConstraint>,
            val body: Body?
        ) : Decl(), WithModifiers {
            override fun toString() = stringRepresentation(
                "Decl.Func", listOf(
                    ArgDesc("mods", mods.toString(), ArgType.LIST),
                    ArgDesc("typeParams", typeParams.toString(), ArgType.LIST),
                    ArgDesc("receiverType", receiverType.toString(), ArgType.VALUE),
                    ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                    ArgDesc("paramTypeParams", paramTypeParams.toString(), ArgType.LIST),
                    ArgDesc("params", params.toString(), ArgType.LIST),
                    ArgDesc("type", type.toString(), ArgType.VALUE),
                    ArgDesc("typeConstraints", typeConstraints.toString(), ArgType.LIST),
                    ArgDesc("body", body.toString(), ArgType.VALUE)
                )
            )

            data class Param(
                override val mods: List<Modifier>,
                val readOnly: Boolean?,
                val name: String,
                // Type can be null for anon functions
                val type: Type?,
                val default: Expr?
            ) : Node(), WithModifiers {
                override fun toString() = stringRepresentation(
                    "Decl.Func.Param", listOf(
                        ArgDesc("mods", mods.toString(), ArgType.LIST),
                        ArgDesc("readOnly", readOnly.toString(), ArgType.VALUE),
                        ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                        ArgDesc("type", type.toString(), ArgType.VALUE),
                        ArgDesc("default", default.toString(), ArgType.VALUE)
                    )
                )
            }

            sealed class Body : Node() {
                data class Block(val block: Node.Block) : Body() {
                    override fun toString() = stringRepresentation(
                        "Decl.Func.Body", listOf(
                            ArgDesc("block", block.toString(), ArgType.VALUE)
                        )
                    )
                }

                data class Expr(val expr: Node.Expr) : Body() {
                    override fun toString() = stringRepresentation(
                        "Decl.Func.Expr", listOf(
                            ArgDesc("expr", expr.toString(), ArgType.VALUE)
                        )
                    )
                }
            }
        }

        data class Property(
            override val mods: List<Modifier>,
            val readOnly: Boolean,
            val typeParams: List<TypeParam>,
            val receiverType: Type?,
            // Always at least one, more than one is destructuring, null is underscore in destructure
            val vars: List<Var?>,
            val typeConstraints: List<TypeConstraint>,
            val delegated: Boolean,
            val expr: Expr?,
            val accessors: Accessors?
        ) : Decl(), WithModifiers {
            override fun toString() = stringRepresentation(
                "Decl.Property", listOf(
                    ArgDesc("mods", mods.toString(), ArgType.LIST),
                    ArgDesc("readOnly", readOnly.toString(), ArgType.VALUE),
                    ArgDesc("typeParams", typeParams.toString(), ArgType.LIST),
                    ArgDesc("receiverType", receiverType.toString(), ArgType.VALUE),
                    ArgDesc("vars", vars.toString(), ArgType.LIST),
                    ArgDesc("typeConstraints", typeConstraints.toString(), ArgType.LIST),
                    ArgDesc("delegated", delegated.toString(), ArgType.VALUE),
                    ArgDesc("expr", expr.toString(), ArgType.VALUE),
                    ArgDesc("accessors", accessors.toString(), ArgType.LIST)
                )
            )

            data class Var(
                val name: String,
                val type: Type?
            ) : Node() {
                override fun toString() = stringRepresentation(
                    "Decl.Property.Var", listOf(
                        ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                        ArgDesc("type", type.toString(), ArgType.VALUE)
                    )
                )
            }

            data class Accessors(
                val first: Accessor,
                val second: Accessor?
            ) : Node() {
                override fun toString() = stringRepresentation(
                    "Decl.Property.Accessors", listOf(
                        ArgDesc("first", first.toString(), ArgType.VALUE),
                        ArgDesc("second", second.toString(), ArgType.VALUE)
                    )
                )
            }

            sealed class Accessor : Node(), WithModifiers {
                data class Get(
                    override val mods: List<Modifier>,
                    val type: Type?,
                    val body: Func.Body?
                ) : Accessor() {
                    override fun toString() = stringRepresentation(
                        "Decl.Property.Accessor.Get", listOf(
                            ArgDesc("mods", mods.toString(), ArgType.LIST),
                            ArgDesc("type", type.toString(), ArgType.VALUE),
                            ArgDesc("body", body.toString(), ArgType.VALUE)
                        )
                    )
                }

                data class Set(
                    override val mods: List<Modifier>,
                    val paramMods: List<Modifier>,
                    val paramName: String?,
                    val paramType: Type?,
                    val body: Func.Body?
                ) : Accessor() {
                    override fun toString() = stringRepresentation(
                        "Decl.Property.Accessor.Set", listOf(
                            ArgDesc("mods", mods.toString(), ArgType.LIST),
                            ArgDesc("paramMods", paramMods.toString(), ArgType.LIST),
                            ArgDesc("paramName", stringifyValue(paramName), ArgType.VALUE),
                            ArgDesc("paramType", paramType.toString(), ArgType.VALUE),
                            ArgDesc("body", body.toString(), ArgType.VALUE)
                        )
                    )
                }
            }
        }

        data class TypeAlias(
            override val mods: List<Modifier>,
            val name: String,
            val typeParams: List<TypeParam>,
            val type: Type
        ) : Decl(), WithModifiers {
            override fun toString() = stringRepresentation(
                "Decl.TypeAlias", listOf(
                    ArgDesc("mods", mods.toString(), ArgType.LIST),
                    ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                    ArgDesc("typeParams", typeParams.toString(), ArgType.LIST),
                    ArgDesc("type", type.toString(), ArgType.VALUE)
                )
            )
        }

        data class Constructor(
            override val mods: List<Modifier>,
            val params: List<Func.Param>,
            val delegationCall: DelegationCall?,
            val block: Block?
        ) : Decl(), WithModifiers {
            override fun toString() = stringRepresentation(
                "Decl.Constructor", listOf(
                    ArgDesc("mods", mods.toString(), ArgType.LIST),
                    ArgDesc("params", params.toString(), ArgType.LIST),
                    ArgDesc("delegationCall", delegationCall.toString(), ArgType.VALUE),
                    ArgDesc("block", block.toString(), ArgType.VALUE)
                )
            )

            data class DelegationCall(
                val target: DelegationTarget,
                val args: List<ValueArg>
            ) : Node() {
                override fun toString() = stringRepresentation(
                    "Decl.Constructor.DelegationCall", listOf(
                        ArgDesc("target", target.toString(), ArgType.VALUE),
                        ArgDesc("args", args.toString(), ArgType.LIST)
                    )
                )
            }

            enum class DelegationTarget {
                THIS, SUPER;

                override fun toString() = PREFIX + "Decl.Constructor.DelegationTarget." + super.toString()
            }
        }

        data class EnumEntry(
            override val mods: List<Modifier>,
            val name: String,
            val args: List<ValueArg>,
            val members: List<Decl>
        ) : Decl(), WithModifiers {
            override fun toString() = stringRepresentation(
                "Decl.EnumEntry", listOf(
                    ArgDesc("mods", mods.toString(), ArgType.LIST),
                    ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                    ArgDesc("args", args.toString(), ArgType.LIST),
                    ArgDesc("members", members.toString(), ArgType.LIST)
                )
            )
        }
    }

    data class TypeParam(
        override val mods: List<Modifier>,
        val name: String,
        val type: TypeRef?
    ) : Node(), WithModifiers {
        override fun toString() = stringRepresentation(
            "TypeParam", listOf(
                ArgDesc("mods", mods.toString(), ArgType.LIST),
                ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                ArgDesc("type", type.toString(), ArgType.VALUE)
            )
        )
    }

    data class TypeConstraint(
        override val anns: List<Modifier.AnnotationSet>,
        val name: String,
        val type: Type
    ) : Node(), WithAnnotations {
        override fun toString() = stringRepresentation(
            "TypeConstraint", listOf(
                ArgDesc("anns", anns.toString(), ArgType.LIST),
                ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                ArgDesc("type", type.toString(), ArgType.VALUE)
            )
        )
    }

    sealed class TypeRef : Node() {
        data class Paren(
            override val mods: List<Modifier>,
            val type: TypeRef
        ) : TypeRef(), WithModifiers {
            override fun toString() = stringRepresentation(
                "TypeRef.Paren", listOf(
                    ArgDesc("mods", mods.toString(), ArgType.LIST),
                    ArgDesc("type", type.toString(), ArgType.VALUE)
                )
            )
        }

        data class Func(
            val receiverType: Type?,
            val params: List<Param>,
            val type: Type
        ) : TypeRef() {
            override fun toString() = stringRepresentation(
                "TypeRef.Func", listOf(
                    ArgDesc("receiverType", receiverType.toString(), ArgType.VALUE),
                    ArgDesc("params", params.toString(), ArgType.LIST),
                    ArgDesc("type", type.toString(), ArgType.VALUE)
                )
            )

            data class Param(
                val name: String?,
                val type: Type
            ) : Node() {
                override fun toString() = stringRepresentation(
                    "TypeRef.Func.Param", listOf(
                        ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                        ArgDesc("type", type.toString(), ArgType.VALUE)
                    )
                )
            }
        }

        data class Simple(
            val pieces: List<Piece>
        ) : TypeRef() {
            override fun toString() = stringRepresentation(
                "TypeRef.Simple", listOf(
                    ArgDesc("pieces", pieces.toString(), ArgType.LIST)
                )
            )

            data class Piece(
                val name: String,
                // Null means any
                val typeParams: List<Type?>
            ) : Node() {
                override fun toString() = stringRepresentation(
                    "TypeRef.Simple.Piece", listOf(
                        ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                        ArgDesc("typeParams", typeParams.toString(), ArgType.LIST)
                    )
                )
            }
        }

        data class Nullable(val type: TypeRef) : TypeRef() {
            override fun toString() = stringRepresentation(
                "TypeRef.Nullable", listOf(
                    ArgDesc("type", type.toString(), ArgType.VALUE)
                )
            )
        }

        data class Dynamic(val _unused_: Boolean = false) : TypeRef() {
            override fun toString() = stringRepresentation(
                "TypeRef.Dynamic", listOf(
                    ArgDesc("_unused_", _unused_.toString(), ArgType.VALUE)
                )
            )
        }
    }

    data class Type(
        override val mods: List<Modifier>,
        val ref: TypeRef
    ) : Node(), WithModifiers {
        override fun toString() = stringRepresentation(
            "Type", listOf(
                ArgDesc("mods", mods.toString(), ArgType.LIST),
                ArgDesc("ref", ref.toString(), ArgType.VALUE)
            )
        )
    }

    data class ValueArg(
        val name: String?,
        val asterisk: Boolean,
        val expr: Expr
    ) : Node() {
        override fun toString() = stringRepresentation(
            "ValueArg", listOf(
                ArgDesc("name", stringifyValue(name), ArgType.VALUE),
                ArgDesc("asterisk", asterisk.toString(), ArgType.VALUE),
                ArgDesc("expr", expr.toString(), ArgType.VALUE)
            )
        )
    }

    sealed class Expr : Node() {
        data class If(
            val expr: Expr,
            val body: Expr,
            val elseBody: Expr?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.If", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE),
                    ArgDesc("body", body.toString(), ArgType.VALUE),
                    ArgDesc("elseBody", elseBody.toString(), ArgType.VALUE)
                )
            )
        }

        data class Try(
            val block: Block,
            val catches: List<Catch>,
            val finallyBlock: Block?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Try", listOf(
                    ArgDesc("block", block.toString(), ArgType.VALUE),
                    ArgDesc("catches", catches.toString(), ArgType.LIST),
                    ArgDesc("elseBody", finallyBlock.toString(), ArgType.VALUE)
                )
            )

            data class Catch(
                override val anns: List<Modifier.AnnotationSet>,
                val varName: String,
                val varType: TypeRef.Simple,
                val block: Block
            ) : Node(), WithAnnotations {
                override fun toString() = stringRepresentation(
                    "Expr.Try.Catch", listOf(
                        ArgDesc("anns", anns.toString(), ArgType.LIST),
                        ArgDesc("varName", stringifyValue(varName), ArgType.VALUE),
                        ArgDesc("varType", varType.toString(), ArgType.VALUE),
                        ArgDesc("block", block.toString(), ArgType.VALUE)
                    )
                )
            }
        }

        data class For(
            override val anns: List<Modifier.AnnotationSet>,
            // More than one means destructure, null means underscore
            val vars: List<Decl.Property.Var?>,
            val inExpr: Expr,
            val body: Expr
        ) : Expr(), WithAnnotations {
            override fun toString() = stringRepresentation(
                "Expr.For", listOf(
                    ArgDesc("anns", anns.toString(), ArgType.LIST),
                    ArgDesc("vars", vars.toString(), ArgType.LIST),
                    ArgDesc("inExpr", inExpr.toString(), ArgType.VALUE),
                    ArgDesc("body", body.toString(), ArgType.VALUE)
                )
            )
        }

        data class While(
            val expr: Expr,
            val body: Expr,
            val doWhile: Boolean
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.While", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE),
                    ArgDesc("body", body.toString(), ArgType.VALUE),
                    ArgDesc("doWhile", doWhile.toString(), ArgType.VALUE)
                )
            )
        }

        data class BinaryOp(
            val lhs: Expr,
            val oper: Oper,
            val rhs: Expr
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.BinaryOp", listOf(
                    ArgDesc("lhs", lhs.toString(), ArgType.VALUE),
                    ArgDesc("oper", oper.toString(), ArgType.VALUE),
                    ArgDesc("rhs", rhs.toString(), ArgType.VALUE)
                )
            )

            sealed class Oper : Node() {
                data class Infix(val str: String) : Oper() {
                    override fun toString() = stringRepresentation(
                        "Expr.BinaryOp.Oper.Infix", listOf(
                            ArgDesc("str", stringifyValue(str), ArgType.VALUE)
                        )
                    )
                }

                data class Token(val token: BinaryOp.Token) : Oper() {
                    override fun toString() = stringRepresentation(
                        "Expr.BinaryOp.Oper.Token", listOf(
                            ArgDesc("token", token.toString(), ArgType.VALUE)
                        )
                    )
                }
            }

            enum class Token(val str: String) {
                MUL("*"), DIV("/"), MOD("%"), ADD("+"), SUB("-"),
                IN("in"), NOT_IN("!in"),
                GT(">"), GTE(">="), LT("<"), LTE("<="),
                EQ("=="), NEQ("!="),
                ASSN("="), MUL_ASSN("*="), DIV_ASSN("/="), MOD_ASSN("%="), ADD_ASSN("+="), SUB_ASSN("-="),
                OR("||"), AND("&&"), ELVIS("?:"), RANGE("stdlib"),
                DOT("."), DOT_SAFE("?."), SAFE("?");

                override fun toString() = PREFIX + "Expr.BinaryOp.Token." + super.toString()
            }
        }

        data class UnaryOp(
            val expr: Expr,
            val oper: Oper,
            val prefix: Boolean
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.UnaryOp", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE),
                    ArgDesc("oper", oper.toString(), ArgType.VALUE),
                    ArgDesc("prefix", prefix.toString(), ArgType.VALUE)
                )
            )

            data class Oper(val token: Token) : Node() {
                override fun toString() = stringRepresentation(
                    "Expr.UnaryOp.Oper", listOf(
                        ArgDesc("token", token.toString(), ArgType.VALUE)
                    )
                )
            }

            enum class Token(val str: String) {
                NEG("-"), POS("+"), INC("++"), DEC("--"), NOT("!"), NULL_DEREF("!!");

                override fun toString() = PREFIX + "Expr.UnaryOp.Token." + super.toString()
            }
        }

        data class TypeOp(
            val lhs: Expr,
            val oper: Oper,
            val rhs: Type
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.TypeOp", listOf(
                    ArgDesc("lhs", lhs.toString(), ArgType.VALUE),
                    ArgDesc("oper", oper.toString(), ArgType.VALUE),
                    ArgDesc("rhs", rhs.toString(), ArgType.VALUE)
                )
            )

            data class Oper(val token: Token) : Node() {
                override fun toString() = stringRepresentation(
                    "Expr.TypeOp.Oper", listOf(
                        ArgDesc("token", token.toString(), ArgType.VALUE)
                    )
                )
            }

            enum class Token(val str: String) {
                AS("as"), AS_SAFE("as?"), COL(":"), IS("is"), NOT_IS("!is");

                override fun toString() = PREFIX + "Expr.TypeOp.Token." + super.toString()
            }
        }

        sealed class DoubleColonRef : Expr() {
            abstract val recv: Recv?

            data class Callable(
                override val recv: Recv?,
                val name: String
            ) : DoubleColonRef() {
                override fun toString() = stringRepresentation(
                    "Expr.DoubleColonRef.Callable", listOf(
                        ArgDesc("recv", recv.toString(), ArgType.VALUE),
                        ArgDesc("name", stringifyValue(name), ArgType.VALUE)
                    )
                )
            }

            data class Class(
                override val recv: Recv?
            ) : DoubleColonRef() {
                override fun toString() = stringRepresentation(
                    "Expr.DoubleColonRef.Class", listOf(
                        ArgDesc("recv", recv.toString(), ArgType.VALUE)
                    )
                )
            }

            sealed class Recv : Node() {
                data class Expr(val expr: Node.Expr) : Recv() {
                    override fun toString() = stringRepresentation(
                        "Expr.DoubleColonRef.Recv.Expr", listOf(
                            ArgDesc("expr", expr.toString(), ArgType.VALUE)
                        )
                    )
                }

                data class Type(
                    val type: TypeRef.Simple,
                    val questionMarks: Int
                ) : Recv() {
                    override fun toString() = stringRepresentation(
                        "Expr.DoubleColonRef.Recv.Type", listOf(
                            ArgDesc("type", type.toString(), ArgType.VALUE),
                            ArgDesc("questionMarks", questionMarks.toString(), ArgType.VALUE)
                        )
                    )
                }
            }
        }

        data class Paren(
            val expr: Expr
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Paren", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE)
                )
            )
        }

        data class StringTmpl(
            val elems: List<Elem>,
            val raw: Boolean
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.StringTmpl", listOf(
                    ArgDesc("elems", elems.toString(), ArgType.LIST),
                    ArgDesc("raw", raw.toString(), ArgType.VALUE)
                )
            )

            sealed class Elem : Node() {
                data class Regular(val str: String) : Elem() {
                    override fun toString() = stringRepresentation(
                        "Expr.StringTmpl.Elem.Regular", listOf(
                            ArgDesc("str", stringifyValue(str), ArgType.VALUE)
                        )
                    )
                }

                data class ShortTmpl(val str: String) : Elem() {
                    override fun toString() = stringRepresentation(
                        "Expr.StringTmpl.Elem.ShortTmpl", listOf(
                            ArgDesc("str", stringifyValue(str), ArgType.VALUE)
                        )
                    )
                }

                data class UnicodeEsc(val digits: String) : Elem() {
                    override fun toString() = stringRepresentation(
                        "Expr.StringTmpl.Elem.UnicodeEsc", listOf(
                            ArgDesc("digits", stringifyValue(digits), ArgType.VALUE)
                        )
                    )
                }

                data class RegularEsc(val char: Char) : Elem() {
                    override fun toString() = stringRepresentation(
                        "Expr.StringTmpl.Elem.RegularEsc", listOf(
                            ArgDesc("char", char.toString(), ArgType.VALUE)
                        )
                    )
                }

                data class LongTmpl(val expr: Expr) : Elem() {
                    override fun toString() = stringRepresentation(
                        "Expr.StringTmpl.Elem.LongTmpl", listOf(
                            ArgDesc("expr", expr.toString(), ArgType.VALUE)
                        )
                    )
                }
            }
        }

        data class Const(
            val value: String,
            val form: Form
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Const", listOf(
                    ArgDesc("value", stringifyValue(value), ArgType.VALUE),
                    ArgDesc("form", form.toString(), ArgType.VALUE)
                )
            )

            enum class Form {
                BOOLEAN, CHAR, INT, FLOAT, NULL;

                override fun toString() = PREFIX + "Expr.Const.Form." + super.toString()
            }
        }

        data class Brace(
            val params: List<Param>,
            val block: Block?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Brace", listOf(
                    ArgDesc("params", params.toString(), ArgType.LIST),
                    ArgDesc("block", block.toString(), ArgType.VALUE)
                )
            )

            data class Param(
                // Multiple means destructure, null means underscore
                val vars: List<Decl.Property.Var?>,
                val destructType: Type?
            ) : Expr() {
                override fun toString() = stringRepresentation(
                    "Expr.Brace.Param", listOf(
                        ArgDesc("vars", vars.toString(), ArgType.LIST),
                        ArgDesc("destructType", destructType.toString(), ArgType.VALUE)
                    )
                )
            }
        }

        data class This(
            val label: String?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.This", listOf(
                    ArgDesc("label", stringifyValue(label), ArgType.VALUE)
                )
            )
        }

        data class Super(
            val typeArg: Type?,
            val label: String?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Super", listOf(
                    ArgDesc("typeArg", typeArg.toString(), ArgType.VALUE),
                    ArgDesc("label", stringifyValue(label), ArgType.VALUE)
                )
            )
        }

        data class When(
            val expr: Expr?,
            val entries: List<Entry>
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.When", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE),
                    ArgDesc("entries", entries.toString(), ArgType.LIST)
                )
            )

            data class Entry(
                val conds: List<Cond>,
                val body: Expr
            ) : Node() {
                override fun toString() = stringRepresentation(
                    "Expr.When.Entry", listOf(
                        ArgDesc("conds", conds.toString(), ArgType.LIST),
                        ArgDesc("body", body.toString(), ArgType.VALUE)
                    )
                )
            }

            sealed class Cond : Node() {
                data class Expr(val expr: Node.Expr) : Cond() {
                    override fun toString() = stringRepresentation(
                        "Expr.When.Cond.Expr", listOf(
                            ArgDesc("expr", expr.toString(), ArgType.VALUE)
                        )
                    )
                }

                data class In(
                    val expr: Node.Expr,
                    val not: Boolean
                ) : Cond() {
                    override fun toString() = stringRepresentation(
                        "Expr.When.Cond.In", listOf(
                            ArgDesc("expr", expr.toString(), ArgType.VALUE),
                            ArgDesc("not", not.toString(), ArgType.VALUE)
                        )
                    )
                }

                data class Is(
                    val type: Type,
                    val not: Boolean
                ) : Cond() {
                    override fun toString() = stringRepresentation(
                        "Expr.When.Cond.Is", listOf(
                            ArgDesc("type", type.toString(), ArgType.VALUE),
                            ArgDesc("not", not.toString(), ArgType.VALUE)
                        )
                    )
                }
            }
        }

        data class Object(
            val parents: List<Decl.Structured.Parent>,
            val members: List<Decl>
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Object", listOf(
                    ArgDesc("parents", parents.toString(), ArgType.LIST),
                    ArgDesc("members", members.toString(), ArgType.LIST)
                )
            )
        }

        data class Throw(
            val expr: Expr
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Throw", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE)
                )
            )
        }

        data class Return(
            val label: String?,
            val expr: Expr?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Return", listOf(
                    ArgDesc("label", stringifyValue(label), ArgType.VALUE),
                    ArgDesc("expr", expr.toString(), ArgType.VALUE)
                )
            )
        }

        data class Continue(
            val label: String?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Continue", listOf(
                    ArgDesc("label", stringifyValue(label), ArgType.VALUE)
                )
            )
        }

        data class Break(
            val label: String?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Break", listOf(
                    ArgDesc("label", stringifyValue(label), ArgType.VALUE)
                )
            )
        }

        data class CollLit(
            val exprs: List<Expr>
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.CollLit", listOf(
                    ArgDesc("exprs", exprs.toString(), ArgType.LIST)
                )
            )
        }

        data class Name(
            val name: String
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Name", listOf(
                    ArgDesc("name", stringifyValue(name), ArgType.VALUE)
                )
            )
        }

        data class Labeled(
            val label: String,
            val expr: Expr
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Labeled", listOf(
                    ArgDesc("label", stringifyValue(label), ArgType.VALUE),
                    ArgDesc("expr", expr.toString(), ArgType.VALUE)
                )
            )
        }

        data class Annotated(
            override val anns: List<Modifier.AnnotationSet>,
            val expr: Expr
        ) : Expr(), WithAnnotations {
            override fun toString() = stringRepresentation(
                "Expr.Annotated", listOf(
                    ArgDesc("anns", anns.toString(), ArgType.LIST),
                    ArgDesc("expr", expr.toString(), ArgType.VALUE)
                )
            )
        }

        data class Call(
            val expr: Expr,
            val typeArgs: List<Type?>,
            val args: List<ValueArg>,
            val lambda: TrailLambda?
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Call", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE),
                    ArgDesc("typeArgs", typeArgs.toString(), ArgType.LIST),
                    ArgDesc("args", args.toString(), ArgType.LIST),
                    ArgDesc("lambda", lambda.toString(), ArgType.VALUE)
                )
            )

            data class TrailLambda(
                override val anns: List<Modifier.AnnotationSet>,
                val label: String?,
                val func: Brace
            ) : Node(), WithAnnotations {
                override fun toString() = stringRepresentation(
                    "Expr.Call.TrailLambda", listOf(
                        ArgDesc("anns", anns.toString(), ArgType.LIST),
                        ArgDesc("label", stringifyValue(label), ArgType.VALUE),
                        ArgDesc("func", func.toString(), ArgType.VALUE)
                    )
                )
            }
        }

        data class ArrayAccess(
            val expr: Expr,
            val indices: List<Expr>
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.ArrayAccess", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE),
                    ArgDesc("indices", indices.toString(), ArgType.LIST)
                )
            )
        }

        data class AnonFunc(
            val func: Decl.Func
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.AnonFunc", listOf(
                    ArgDesc("func", func.toString(), ArgType.VALUE)
                )
            )
        }

        // This is only present for when expressions and labeled expressions
        data class Property(
            val decl: Decl.Property
        ) : Expr() {
            override fun toString() = stringRepresentation(
                "Expr.Property", listOf(
                    ArgDesc("decl", decl.toString(), ArgType.VALUE)
                )
            )
        }
    }

    data class Block(val stmts: List<Stmt>) : Node() {
        override fun toString() = stringRepresentation(
            "Block", listOf(
                ArgDesc("stmts", stmts.toString(), ArgType.LIST)
            )
        )
    }

    sealed class Stmt : Node() {
        data class Decl(val decl: Node.Decl) : Stmt() {
            override fun toString() = stringRepresentation(
                "Stmt.Decl", listOf(
                    ArgDesc("decl", decl.toString(), ArgType.VALUE)
                )
            )
        }

        data class Expr(val expr: Node.Expr) : Stmt() {
            override fun toString() = stringRepresentation(
                "Stmt.Expr", listOf(
                    ArgDesc("expr", expr.toString(), ArgType.VALUE)
                )
            )
        }
    }

    sealed class Modifier : Node() {
        data class AnnotationSet(
            val target: Target?,
            val anns: List<Annotation>
        ) : Modifier() {
            override fun toString() = stringRepresentation(
                "Modifier.AnnotationSet", listOf(
                    ArgDesc("target", target.toString(), ArgType.VALUE),
                    ArgDesc("anns", anns.toString(), ArgType.LIST)
                )
            )

            enum class Target {
                FIELD, FILE, PROPERTY, GET, SET, RECEIVER, PARAM, SETPARAM, DELEGATE;

                override fun toString() = PREFIX + "Modifier.AnnotationSet.Target." + super.toString()
            }

            data class Annotation(
                val names: List<String>,
                val typeArgs: List<Type>,
                val args: List<ValueArg>
            ) : Node() {
                override fun toString() = stringRepresentation(
                    "Modifier.AnnotationSet.Annotation", listOf(
                        ArgDesc("names", stringifyList(names), ArgType.LIST),
                        ArgDesc("typeArgs", typeArgs.toString(), ArgType.LIST),
                        ArgDesc("args", args.toString(), ArgType.LIST)
                    )
                )
            }
        }

        data class Lit(val keyword: Keyword) : Modifier() {
            override fun toString() = stringRepresentation(
                "Modifier.Lit", listOf(
                    ArgDesc("keyword", keyword.toString(), ArgType.VALUE)
                )
            )
        }

        enum class Keyword {
            ABSTRACT, FINAL, OPEN, ANNOTATION, SEALED, DATA, OVERRIDE, LATEINIT, INNER,
            PRIVATE, PROTECTED, PUBLIC, INTERNAL,
            IN, OUT, NOINLINE, CROSSINLINE, VARARG, REIFIED,
            TAILREC, OPERATOR, INFIX, INLINE, EXTERNAL, SUSPEND, CONST,
            ACTUAL, EXPECT;

            override fun toString() = PREFIX + "Modifier.Keyword" + super.toString()
        }
    }

    sealed class Extra : Node() {
        data class BlankLines(
            val count: Int
        ) : Extra() {
            override fun toString() = stringRepresentation(
                "Extra.BlankLines", listOf(
                    ArgDesc("count", count.toString(), ArgType.VALUE)
                )
            )
        }

        data class Comment(
            val text: String,
            val startsLine: Boolean,
            val endsLine: Boolean
        ) : Extra() {
            override fun toString() = stringRepresentation(
                "Extra.Comment", listOf(
                    ArgDesc("text", stringifyValue(text), ArgType.VALUE),
                    ArgDesc("startsLine", startsLine.toString(), ArgType.VALUE),
                    ArgDesc("endsLine", endsLine.toString(), ArgType.VALUE)
                )
            )
        }
    }
}