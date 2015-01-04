package decaf.dataflow;

import java.io.PrintWriter;
import java.util.*;

import decaf.machdesc.Asm;
import decaf.machdesc.Register;
import decaf.tac.Label;
import decaf.tac.Tac;
import decaf.tac.Temp;

public class BasicBlock {
	public int bbNum;

	public enum EndKind {
		BY_BRANCH, BY_BEQZ, BY_BNEZ, BY_RETURN
	}

	public EndKind endKind;

	public int inDegree;

	public Tac tacList;

	public Label label;

	public Temp var;

	public Tac varTac;

	public Register varReg;

	public int[] next;

	public boolean cancelled;

	public boolean mark;

	public Set<Map.Entry<Tac, Temp>> def;

	public Set<Map.Entry<Tac, Temp>> liveUse;

	public Set<Map.Entry<Tac, Temp>> LiveIn;

	public Set<Map.Entry<Tac, Temp>> LiveOut;

	public Set<Temp> liveIn;

	public Set<Temp> liveOut;

	public Set<Temp> saves;

	private List<Asm> asms;

	public static final Comparator<Map.Entry<Tac, Temp>> TAC_ID_COMPARATOR = new Comparator<Map.Entry<Tac, Temp>>() {

		@Override
		public int compare(Map.Entry<Tac, Temp> o1, Map.Entry<Tac, Temp> o2) {
			if (o1.getKey().getLineNumber() == o2.getKey().getLineNumber()) {
				return o1.getValue().id > o2.getValue().id ? 1 : o1.getValue().id == o2.getValue().id ? 0 : -1;
			} else {
				return o1.getKey().getLineNumber() > o2.getKey().getLineNumber() ? 1 : -1;
			}
		}

	};

	public BasicBlock() {
		def = new TreeSet<Map.Entry<Tac, Temp>>(TAC_ID_COMPARATOR);
		liveUse = new TreeSet<Map.Entry<Tac, Temp>>(TAC_ID_COMPARATOR);
		LiveIn = new TreeSet<Map.Entry<Tac, Temp>>(TAC_ID_COMPARATOR);
		LiveOut = new TreeSet<Map.Entry<Tac, Temp>>(TAC_ID_COMPARATOR);
		next = new int[2];
		asms = new ArrayList<Asm>();
	}

	public void computeDefAndLiveUse() {
		for (Tac tac = tacList; tac != null; tac = tac.next) {
			switch (tac.opc) {
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
			case LAND:
			case LOR:
			case GTR:
			case GEQ:
			case EQU:
			case NEQ:
			case LEQ:
			case LES:
				/* use op1 and op2, def op0 */
				if (tac.op1.lastVisitedBB != bbNum) {
					liveUse.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op1));
					tac.op1.lastVisitedBB = bbNum;
				}
				if (tac.op2.lastVisitedBB != bbNum) {
					liveUse.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op2));
					tac.op2.lastVisitedBB = bbNum;
				}
				if (tac.op0.lastVisitedBB != bbNum) {
					def.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op0));
					tac.op0.lastVisitedBB = bbNum;
				}
				break;
			case NEG:
			case LNOT:
			case ASSIGN:
			case INDIRECT_CALL:
			case LOAD:
				/* use op1, def op0 */
				if (tac.op1.lastVisitedBB != bbNum) {
					liveUse.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op1));
					tac.op1.lastVisitedBB = bbNum;
				}
				if (tac.op0 != null && tac.op0.lastVisitedBB != bbNum) {  // in INDIRECT_CALL with return type VOID,
					// tac.op0 is null
					def.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op0));
					tac.op0.lastVisitedBB = bbNum;
				}
				break;
			case LOAD_VTBL:
			case DIRECT_CALL:
			case RETURN:
			case LOAD_STR_CONST:
			case LOAD_IMM4:
				/* def op0 */
				if (tac.op0 != null && tac.op0.lastVisitedBB != bbNum) {  // in DIRECT_CALL with return type VOID,
					// tac.op0 is null
					def.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op0));
					tac.op0.lastVisitedBB = bbNum;
				}
				break;
			case STORE:
				/* use op0 and op1*/
				if (tac.op0.lastVisitedBB != bbNum) {
					liveUse.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op0));
					tac.op0.lastVisitedBB = bbNum;
				}
				if (tac.op1.lastVisitedBB != bbNum) {
					liveUse.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op1));
					tac.op1.lastVisitedBB = bbNum;
				}
				break;
			case PARM:
				/* use op0 */
				if (tac.op0.lastVisitedBB != bbNum) {
					liveUse.add(new AbstractMap.SimpleEntry<Tac, Temp>(tac, tac.op0));
					tac.op0.lastVisitedBB = bbNum;
				}
				break;
			default:
				/* BRANCH MEMO MARK PARM*/
				break;
			}
		}
		if (var != null && var.lastVisitedBB != bbNum) {
			liveUse.add(new AbstractMap.SimpleEntry<Tac, Temp>(varTac, var));
			var.lastVisitedBB = bbNum;
		}
		LiveIn.addAll(liveUse);
	}

	public void analyzeLiveness() {
		if (tacList == null)
			return;
		Tac tac = tacList;
		for (; tac.next != null; tac = tac.next); 
		convertTempSet();
		tac.liveOut = new HashSet<Temp> (liveOut);
		if (var != null)
			tac.liveOut.add (var);
		for (; tac != tacList; tac = tac.prev) {
			tac.prev.liveOut = new HashSet<Temp> (tac.liveOut);
			switch (tac.opc) {
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
			case LAND:
			case LOR:
			case GTR:
			case GEQ:
			case EQU:
			case NEQ:
			case LEQ:
			case LES:
				/* use op1 and op2, def op0 */
				tac.prev.liveOut.remove (tac.op0);
				tac.prev.liveOut.add (tac.op1);
				tac.prev.liveOut.add (tac.op2);
				break;
			case NEG:
			case LNOT:
			case ASSIGN:
			case INDIRECT_CALL:
			case LOAD:
				/* use op1, def op0 */
				tac.prev.liveOut.remove (tac.op0);
				tac.prev.liveOut.add (tac.op1);
				break;
			case LOAD_VTBL:
			case DIRECT_CALL:
			case RETURN:
			case LOAD_STR_CONST:
			case LOAD_IMM4:
				/* def op0 */
				tac.prev.liveOut.remove (tac.op0);
				break;
			case STORE:
				/* use op0 and op1*/
				tac.prev.liveOut.add (tac.op0);
				tac.prev.liveOut.add (tac.op1);
				break;
			case BEQZ:
			case BNEZ:
			case PARM:
				/* use op0 */
				tac.prev.liveOut.add (tac.op0);
				break;
			default:
				/* BRANCH MEMO MARK PARM*/
				break;
			}
		}
	}

	public void analyzeDU() {
		
	}

	public void printTo(PrintWriter pw) {
		pw.println("BASIC BLOCK " + bbNum + " : ");
		for (Tac t = tacList; t != null; t = t.next) {
			pw.println("    " + t);
		}
		switch (endKind) {
		case BY_BRANCH:
			pw.println("END BY BRANCH, goto " + next[0]);
			break;
		case BY_BEQZ:
			pw.println("END BY BEQZ, if " + var.name + " = ");
			pw.println("    0 : goto " + next[0] + "; 1 : goto " + next[1]);
			break;
		case BY_BNEZ:
			pw.println("END BY BGTZ, if " + var.name + " = ");
			pw.println("    1 : goto " + next[0] + "; 0 : goto " + next[1]);
			break;
		case BY_RETURN:
			if (var != null) {
				pw.println("END BY RETURN, result = " + var.name);
			} else {
				pw.println("END BY RETURN, void result");
			}
			break;
		}
	}

	public void printLivenessTo(PrintWriter pw) {
		pw.println("BASIC BLOCK " + bbNum + " : ");
		pw.println("  Def     = " + toString(def));
		pw.println("  liveUse = " + toString(liveUse));
		pw.println("  liveIn  = " + toString(LiveIn));
		pw.println("  liveOut = " + toString(LiveOut));

		for (Tac t = tacList; t != null; t = t.next) {
			pw.println("    " + t + " " + oriToString(t.liveOut));
		}

		switch (endKind) {
		case BY_BRANCH:
			pw.println("END BY BRANCH, goto " + next[0]);
			break;
		case BY_BEQZ:
			pw.println("END BY BEQZ, if " + var.name + " = ");
			pw.println("    0 : goto " + next[0] + "; 1 : goto " + next[1]);
			break;
		case BY_BNEZ:
			pw.println("END BY BGTZ, if " + var.name + " = ");
			pw.println("    1 : goto " + next[0] + "; 0 : goto " + next[1]);
			break;
		case BY_RETURN:
			if (var != null) {
				pw.println("END BY RETURN, result = " + var.name);
			} else {
				pw.println("END BY RETURN, void result");
			}
			break;
		}
	}

	public void printDUTo(PrintWriter pw) {

	}

	public String toString(Set<Map.Entry<Tac, Temp>> set) {
		StringBuilder sb = new StringBuilder("[ ");
		for (Map.Entry<Tac, Temp> t : set) {
			sb.append(t.getValue().name + " ");
		}
		sb.append(']');
		return sb.toString();
	}

	public String oriToString(Set<Temp> set) {
		StringBuilder sb = new StringBuilder("[ ");
		for (Temp t : set) {
			sb.append(t.name + " ");
		}
		sb.append(']');
		return sb.toString();
	}

	public void insertBefore(Tac insert, Tac base) {
		if (base == tacList) {
			tacList = insert;
		} else {
			base.prev.next = insert;
		}
		insert.prev = base.prev;
		base.prev = insert;
		insert.next = base;
	}

	public void insertAfter(Tac insert, Tac base) {
		if (tacList == null) {
			tacList = insert;
			insert.next = null;
			return;
		}
		if (base.next != null) {
			base.next.prev = insert;
		}
		insert.prev = base;
		insert.next = base.next;
		base.next = insert;
	}

	public void appendAsm(Asm asm) {
		asms.add(asm);
	}

	public List<Asm> getAsms() {
		return asms;
	}

	public void convertTempSet() {
		liveIn = new TreeSet<Temp>(Temp.ID_COMPARATOR);
		liveOut = new TreeSet<Temp>(Temp.ID_COMPARATOR);
		for (Map.Entry<Tac, Temp> t : LiveOut) {
			liveOut.add(t.getValue());
		}
		for (Map.Entry<Tac, Temp> t : LiveIn) {
			liveIn.add(t.getValue());
		}
	}
}
