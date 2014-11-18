#编译原理 2014秋 第二次实验报告
**计科20 张阳坤 2012012436**
##实验内容
2. 增加对`++`,`--`两个运算符的类型检查
3. 增加对三元操作符 `cond ? trueResult : falseResult`的类型检查
4. 增加 switch-case 结构的语义分析
5. 增加 repeat-until 结构的语义分析

##实现细节
1. 对于 `++`,`--`这两个一元运算符，直接修改一元运算符的类型检查函数，增加对应的运算，在检查时，首先判断变量是否是一个 `LValue` 若否，则抛出`BadLValueError`错误；之后判断变量是否为 `int`，若否，则抛出`IncompatUnOpError`错误同时指明是哪一种运算符导致的错误。
2. 对于三元运算符 `cond ? trueResult : falseResult`，首先判断`cond`是否为合法的条件，可直接调用`checkTestExpr(cond)`；之后检查`trueResult`和`falseResult`的类型是否可以匹配，即
		
		trueResult.type.compatible(falseResult.type)
		falseResult.type.compatible(trueResult.type)
最终输出两者都能被匹配的类型。
3. 为实现 switch-case结构，首先在`typeCheck.BuilfSym`中添加对应的两种方法，指定每一个CaseBlock为一个作用域。在`typeCheck.TypeCheck`中添加对应两种方法，验证`case`后的为一常量，`switch(a)`中`a`为`int`类型。
4. 为实现 repeat-until 结构，在`typeCheck.BuilfSym`和`typeCheck.TypeCheck`中添加对应的方法，利用`checkTestExpr(cond)`验证终止条件是`bool`类型即可。
