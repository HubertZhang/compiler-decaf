#编译原理 2014秋 第一次实验报告
**计科20 张阳坤 2012012436**
##实验内容
1. 增加C-style 的多行注释 /\*...\*/
2. 增加"++", "--" 两个运算符
3. 增加三元操作符 cond ? trueResult : falseResult
4. 实现 switch-case 结构
5. 实现 repeat-until 结构

##实现细节
1. 多行注释是通过在 Lexer.l 中添加一个新的状态 "C" 实现的。在YYINITIAL状态下，如遇到"/\*"，则进入 C 状态，遇到 "\*/"则回归YYINITIAL状态，同时忽略中间的语句。若在 C 状态下遇到文件结尾，则报出错误 UntermMCError。
2. "++", "--" 运算符通过在 Paser.y 中添加一个新的文法实现。形如 A ++, A --, ++ A, -- A的格式被定义为 OperatorExpr。同时在 Expr文法，SimpleStmt文法中加入OperatorExpr。亦即这样的表达式可以是一条完整语句，也可以作为一个表达式构成其他语句。这样的实现使得 

		A++;
	这样的语法得以实现。
3. 三元操作符 cond ? trueResult : falseResult 是作为一条表达式出现的，即

		Expr	:	...
				|	Expr '?' Expr ':' Expr
				...
	同时，为了正确生成语法树，Tree.java中需添加对应的TrianyTree子类。该子类有三个节点，分别对应 Cond, trueResult, falseResult。
4.  为实现 switch-case结构，需增加如下关键字
		
		switch case default  	
	定义如下三种文法：
	
		SwitchBlock		:	SWITCH '(' Expr ')' '{' SwitchCaseList '}'
						;

		SwitchCase		:	CASE Expr ':' StmtList
						|	DEFAULT ':' StmtList
						;

		SwitchCaseList	:	SwitchCaseList SwitchCase
                		|	/* empty */
                		;
    同时定义 SwitchBlock 为 Stmt 的一种，另需在Tree.java中创建对应子类

		public static class SwitchCase extends Tree {...}
		public static class Switch extends Tree {...}
4.  为实现 repeat-until 结构，需增加如下关键字
		
		repeat until  	
	定义如下文法：
	
		RepeatBlock		:	REPEAT Stmt UNTIL Expr ';'
    并定义 RepeatBlock 为 Stmt 的一种，另需在Tree.java中创建对应子类
    
		public static class Repeat extends Tree {...}

