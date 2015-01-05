#编译原理 2014秋 第四次实验报告
**计科20 张阳坤 2012012436**
##实验内容
1. 增加DU链的计算和输出

##实现细节
###  DU链的计算
由于`Backend`类中引用了`BasicBlock`的`liveIn`和`liveOut`成员变量，在实验中无法在不修改`Backend`的同时扩展`liveIn`和`liveOut`。为计算DU链，需增加对应的`LiveUse`,`LiveIn`和`LiveOut`三个成员变量，其中储存`Entry<Tac, Temp>`类型的数据，即在记录变量定值点或引用的同时记录该变量的位置。

`LiveUse`的计算可与`liveUse`同时进行，即在给`liveUse`增添变量的同时给`LiveUse`增添对应的`(Tac, Temp)`元素：

	liveUse.add(tac.op0);
	LiveUse.add(new Entry<Tac, Temp>(tac, tac.op0));
`LiveOut`和`LiveIn`的计算类似`liveIn`和`liveOut`，因此也可同时进行迭代。但此时与计算`Def`的差集的过程需稍微修改，应当删去`LiveOut`中所有含有`Def`中变量的元素，即：

	init a new Set, tempSet
	
	for (Entry<Tac, Temp> t in LiveOut) {
		if (t.Temp is in Def) {
			add t to tempSet
		}
	}
	remove tempSet from LiveOut

### DU链输出格式说明
为了更加方便的输出，我在`Option`中增添了`LEVEL5`用于打印DU链的分析结果，在运行时使用

	java -jar decaf.jar -l 5 input_file
即可

后文以`t4.decaf`中`Main.tester`函数的DU链输出来介绍DU链的格式。打印DU链时会打印每一个Block的每一条语句，同时在语句之后的中括号中打印引用了该变量的语句标示。对于每一个`Basic Block`中的语句，以`Block编号:Block中行号`作为该语句的标示。

	//t4.du
	FUNCTION _Main.tester : 
    BASIC BLOCK 0 : 
        0:1  _T7 = *(_T0 + 8)  [ ]
        0:2  _T8 = 1  [ 0:4 2:2 ]
        /* _T8在此处被定值，在之后第四行和 块2行2 的语句中被引用 */
        0:3  _T9 = 0  [ 0:4 ]
        0:4  _T10 = (_T8 < _T9)  [ ]
    END BY BEQZ, if _T10 = 
        0 : goto 2; 1 : goto 1
    BASIC BLOCK 1 : 
        1:1  _T11 = "Decaf runtime error: Cannot create negative-sized array\n"  [ 1:2 ]
        1:2  parm _T11  [ ]
        1:3  call _PrintString  [ ]
        1:4  call _Halt  [ ]
    END BY BRANCH, goto 2
    BASIC BLOCK 2 : 
        2:1  _T12 = 4  [ 3:1 2:3 2:2 4:1 ]
        2:2  _T13 = (_T12 * _T8)  [ 2:3 ]
        /* _T8在此处被引用 */
        2:3  _T14 = (_T12 + _T13)  [ 2:8 3:1 2:4 ]
        2:4  parm _T14  [ ]
        2:5  _T15 =  call _Alloc  [ 2:8 2:6 ]
        2:6  *(_T15 + 0) = _T8  [ ]
        2:7  _T16 = 0  [ 4:2 ]
        2:8  _T15 = (_T15 + _T14)  [ 5:1 4:1 ]
    END BY BRANCH, goto 3