#编译原理 2014秋 第三次实验报告
**计科20 张阳坤 2012012436**
##实验内容
2. 完成对`++`,`--`两个运算符的表达式翻译工作
3. 增加对三元操作符 `cond ? trueResult : falseResult`的语句翻译
4. 完成 switch-case 结构的语句翻译
5. 完成 repeat-until 结构的语句翻译
6. 去除`instanceof`支持

##实现细节
1.  `++`,`--`一元运算符：首先在`Translator`类中添加对应的语句翻译，即根据返回结果的不同，创建临时的变量存储返回结果，对相应变量进行操作，。如 `a++`:

		Temp dst = Temp.createTempI4();
		Temp add = Temp.createTempI4();
        append(Tac.genAssign(dst, src));
        append(Tac.genLoadImm4(add, Temp.createConstTemp(1)));
        append(Tac.genAdd(src, src, add));
        return dst;
和`++a`:

		Temp dst = Temp.createTempI4();
        Temp add = Temp.createTempI4();
        append(Tac.genLoadImm4(add, Temp.createConstTemp(1)));
        append(Tac.genAdd(src, src, add));
        append(Tac.genAssign(dst, src));
        return dst;
这样保证了`a++`返回值为`a`而`++a`返回值为`a+1`，且`a`的值被赋值为`a+1`。之后在`TransPass2`中添加对应的一元运算符的翻译即可
2. 三元运算符 `cond ? trueResult : falseResult`：在`TransPass2`中添加三元运算符的结构即可，大致实现为根据`cond`的真值跳转到对应的赋值语句，给变量附上对应的值。

        calculate cond
        calculate trueResult
        calculate falseResult
        
        if (!cond) goto falseLabel
        assign trueResult
        goto exitLabel
        
        falseLabel:
        assign falseResult

		exitLabel:
        ...
3. switch-case 结构：在`TransPass2`中添加对应的结构。因为暂无法获知具体的数据信息，不宜使用跳转表，在这里使用了如下的类似链表的结构

        calculate cond
		if (cond != caseConst) goto caseEnd
		caseBody...
        caseEnd:
        
        if (cond != caseConst) goto caseEnd
		caseBody...
        caseEnd:
        ...
        
        exitLabel:
		...
4. repeat-until 结构：类似的，在`TransPass2`中添加对应的结构。

        loopBegin:
        loopBoby...
        
        calculate cond
        if (!cond) goto loopBegin
        
        exitLabel:
        ...
5. 去除`instanceof`支持：去除如下位置的代码：

		前端中instanceof代码：包括Lexer.l，Paser.y，SemValue.java这三个文件
		Tree类中 TypeTest子类的相关代码：包括Tree下子类，TypeCheck类和TransPass2类中的visitTypeTest函数
		VTable类：parent成员变量
		Translator类： genInstanceof函数
		TransPass1类： visitTopLevel函数中引用到VTable.parent的内容
		Mips类： emitVTable函数中引用到VTable.parent的内容
详情可以参考后面附上的[diff文件](https://gist.github.com/Hubertzhang/8298cf75f348b230d257)

		From 77b1dc58f28eaa6759ca94c56639058adcf68786 Mon Sep 17 00:00:00 2001
        From: Hubertzhang <hubert.zyk@gmail.com>
        Date: Thu, 18 Dec 2014 16:38:04 +0800
        Subject: [PATCH] Remove instanceof

        ---
         src/decaf/backend/Mips.java         |  2 --
         src/decaf/frontend/Lexer.l          |  1 -
         src/decaf/frontend/Parser.y         |  6 +-----
         src/decaf/frontend/SemValue.java    |  3 ---
         src/decaf/tac/VTable.java           |  2 --
         src/decaf/translate/TransPass1.java |  6 ------
         src/decaf/translate/TransPass2.java |  7 -------
         src/decaf/translate/Translater.java | 21 ---------------------
         src/decaf/tree/Tree.java            | 34 ----------------------------------
         src/decaf/typecheck/TypeCheck.java  | 16 ----------------
         10 files changed, 1 insertion(+), 97 deletions(-)

        diff --git a/src/decaf/backend/Mips.java b/src/decaf/backend/Mips.java
        index 11741e0..64d70a0 100755
        --- a/src/decaf/backend/Mips.java
        +++ b/src/decaf/backend/Mips.java
        @@ -345,8 +345,6 @@ public class Mips implements MachineDescription {
         			emit(null, ".data", null);
         			emit(null, ".align 2", null);
         			emit(vt.name, null, "virtual table");
        -			emit(null, ".word " + (vt.parent == null ? "0" : vt.parent.name),
        -					"parent");
         			emit(null, ".word " + getStringConstLabel(vt.className),
         					"class name");
         			for (Label l : vt.entries) {
        diff --git a/src/decaf/frontend/Lexer.l b/src/decaf/frontend/Lexer.l
        index 1b76363..be6957c 100755
        --- a/src/decaf/frontend/Lexer.l
        +++ b/src/decaf/frontend/Lexer.l
        @@ -73,7 +73,6 @@ WHITESPACE			= ([ \t]+)
         "ReadInteger"		{ return keyword(Parser.READ_INTEGER);	}
         "ReadLine"			{ return keyword(Parser.READ_LINE);		}
         "static"			{ return keyword(Parser.STATIC);		}
        -"instanceof"		{ return keyword(Parser.INSTANCEOF);	}
         "switch"            { return keyword(Parser.SWITCH);        }
         "case"              { return keyword(Parser.CASE);          }
         "default"           { return keyword(Parser.DEFAULT);       }
        diff --git a/src/decaf/frontend/Parser.y b/src/decaf/frontend/Parser.y
        index f44c023..5d616a6 100755
        --- a/src/decaf/frontend/Parser.y
        +++ b/src/decaf/frontend/Parser.y
        @@ -29,7 +29,7 @@ import java.util.*;
         %token IF     ELSE        RETURN   BREAK   NEW
         %token PRINT  READ_INTEGER         READ_LINE
         %token LITERAL
        -%token IDENTIFIER	  AND    OR    STATIC  INSTANCEOF
        +%token IDENTIFIER	  AND    OR    STATIC
         %token SWITCH CASE DEFAULT REPEAT UNTIL
         %token LESS_EQUAL   GREATER_EQUAL  EQUAL   NOT_EQUAL
         %token INCREASE DECREASE
        @@ -367,10 +367,6 @@ Expr            :	LValue
                         	{
                         		$$.expr = new Tree.NewArray($2.type, $4.expr, $1.loc);
                         	}
        -                |	INSTANCEOF '(' Expr ',' IDENTIFIER ')'
        -                	{
        -                		$$.expr = new Tree.TypeTest($3.expr, $5.ident, $1.loc);
        -                	}
                         |	'(' CLASS IDENTIFIER ')' Expr
                         	{
                         		$$.expr = new Tree.TypeCast($3.ident, $5.expr, $5.loc);
        diff --git a/src/decaf/frontend/SemValue.java b/src/decaf/frontend/SemValue.java
        index 64e9d7f..2c9f71b 100755
        --- a/src/decaf/frontend/SemValue.java
        +++ b/src/decaf/frontend/SemValue.java
        @@ -146,9 +146,6 @@ public class SemValue {
         		case Parser.INT:
         			msg = "keyword  : int";
         			break;
        -		case Parser.INSTANCEOF:
        -			msg = "keyword  : instanceof";
        -			break;
         		case Parser.NEW:
         			msg = "keyword  : new";
         			break;
        diff --git a/src/decaf/tac/VTable.java b/src/decaf/tac/VTable.java
        index 97552ac..d538cc6 100755
        --- a/src/decaf/tac/VTable.java
        +++ b/src/decaf/tac/VTable.java
        @@ -3,8 +3,6 @@ package decaf.tac;
         public class VTable {
         	public String name;
         	
        -	public VTable parent;
        -	
         	public String className;
         
         	public Label[] entries;
        diff --git a/src/decaf/translate/TransPass1.java b/src/decaf/translate/TransPass1.java
        index f8adb31..3d814f8 100755
        --- a/src/decaf/translate/TransPass1.java
        +++ b/src/decaf/translate/TransPass1.java
        @@ -33,12 +33,6 @@ public class TransPass1 extends Tree.Visitor {
         			tr.createVTable(cd.symbol);
         			tr.genNewForClass(cd.symbol);
         		}
        -		for (Tree.ClassDef cd : program.classes) {
        -			if (cd.parent != null) {
        -				cd.symbol.getVtable().parent = cd.symbol.getParent()
        -						.getVtable();
        -			}
        -		}
         	}
         
         	@Override
        diff --git a/src/decaf/translate/TransPass2.java b/src/decaf/translate/TransPass2.java
        index 5193d3e..f51e72a 100755
        --- a/src/decaf/translate/TransPass2.java
        +++ b/src/decaf/translate/TransPass2.java
        @@ -446,13 +446,6 @@ public class TransPass2 extends Tree.Visitor {
                 tr.genMark(exit);
             }
         
        -    @Override
        -	public void visitTypeTest(Tree.TypeTest typeTest) {
        -		typeTest.instance.accept(this);
        -		typeTest.val = tr.genInstanceof(typeTest.instance.val,
        -				typeTest.symbol);
        -	}
        -
         	@Override
         	public void visitTypeCast(Tree.TypeCast typeCast) {
         		typeCast.expr.accept(this);
        diff --git a/src/decaf/translate/Translater.java b/src/decaf/translate/Translater.java
        index 982ba13..5f8b89e 100755
        --- a/src/decaf/translate/Translater.java
        +++ b/src/decaf/translate/Translater.java
        @@ -46,11 +46,6 @@ public class Translater {
         	public void printTo(PrintWriter pw) {
         		for (VTable vt : vtables) {
         			pw.println("VTABLE(" + vt.name + ") {");
        -			if (vt.parent != null) {
        -				pw.println("    " + vt.parent.name);
        -			} else {
        -				pw.println("    <empty>");
        -			}
         			pw.println("    " + vt.className);
         			for (Label l : vt.entries) {
         				pw.println("    " + l.name + ";");
        @@ -464,22 +459,6 @@ public class Translater {
         		endFunc();
         	}
         
        -	public Temp genInstanceof(Temp instance, Class c) {
        -		Temp dst = Temp.createTempI4();
        -		Label loop = Label.createLabel();
        -		Label exit = Label.createLabel();
        -		Temp targetVp = genLoadVTable(c.getVtable());
        -		Temp vp = genLoad(instance, 0);
        -		genMark(loop);
        -		append(Tac.genEqu(dst, targetVp, vp));
        -		genBnez(dst, exit);
        -		append(Tac.genLoad(vp, vp, Temp.createConstTemp(0)));
        -		genBnez(vp, loop);
        -		append(Tac.genLoadImm4(dst, Temp.createConstTemp(0)));
        -		genMark(exit);
        -		return dst;
        -	}
        -
         	public void genClassCast(Temp val, Class c) {
         		Label loop = Label.createLabel();
         		Label exit = Label.createLabel();
        diff --git a/src/decaf/tree/Tree.java b/src/decaf/tree/Tree.java
        index a3af4fa..2a3ab53 100755
        --- a/src/decaf/tree/Tree.java
        +++ b/src/decaf/tree/Tree.java
        @@ -1260,36 +1260,6 @@ public abstract class Tree {
             }
         
             /**
        -      * instanceof expression
        -      */
        -    public static class TypeTest extends Expr {
        -    	
        -    	public Expr instance;
        -    	public String className;
        -    	public Class symbol;
        -
        -        public TypeTest(Expr instance, String className, Location loc) {
        -            super(TYPETEST, loc);
        -    		this.instance = instance;
        -    		this.className = className;
        -        }
        -
        -    	@Override
        -        public void accept(Visitor v) {
        -            v.visitTypeTest(this);
        -        }
        -
        -    	@Override
        -    	public void printTo(IndentPrintWriter pw) {
        -    		pw.println("instanceof");
        -    		pw.incIndent();
        -    		instance.printTo(pw);
        -    		pw.println(className);
        -    		pw.decIndent();
        -    	}
        -    }
        -
        -    /**
               * An array selection
               */
             public static class Indexed extends LValue {
        @@ -1616,10 +1586,6 @@ public abstract class Tree {
                     visitTree(that);
                 }
         
        -        public void visitTypeTest(TypeTest that) {
        -            visitTree(that);
        -        }
        -
                 public void visitIndexed(Indexed that) {
                     visitTree(that);
                 }
        diff --git a/src/decaf/typecheck/TypeCheck.java b/src/decaf/typecheck/TypeCheck.java
        index 5c8e323..be32294 100755
        --- a/src/decaf/typecheck/TypeCheck.java
        +++ b/src/decaf/typecheck/TypeCheck.java
        @@ -318,22 +318,6 @@ public class TypeCheck extends Tree.Visitor {
         	}
         
         	@Override
        -	public void visitTypeTest(Tree.TypeTest instanceofExpr) {
        -		instanceofExpr.instance.accept(this);
        -		if (!instanceofExpr.instance.type.isClassType()) {
        -			issueError(new NotClassError(instanceofExpr.instance.type
        -					.toString(), instanceofExpr.getLocation()));
        -		}
        -		Class c = table.lookupClass(instanceofExpr.className);
        -		instanceofExpr.symbol = c;
        -		instanceofExpr.type = BaseType.BOOL;
        -		if (c == null) {
        -			issueError(new ClassNotFoundError(instanceofExpr.getLocation(),
        -					instanceofExpr.className));
        -		}
        -	}
        -
        -	@Override
         	public void visitTypeCast(Tree.TypeCast cast) {
         		cast.expr.accept(this);
         		if (!cast.expr.type.isClassType()) {
        -- 
        1.8.4.2



