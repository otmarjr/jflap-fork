/*
 *  JFLAP - Formal Languages and Automata Package
 * 
 * 
 *  Susan H. Rodger
 *  Computer Science Department
 *  Duke University
 *  August 27, 2009

 *  Copyright (c) 2002-2009
 *  All rights reserved.

 *  JFLAP is open source software. Please see the LICENSE for terms.
 *
 */
package automata.fsa;

import automata.Automaton;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This subclass of <CODE>Automaton</CODE> is specifically for a definition of a
 * regular Finite State Automaton.
 *
 * @author Thomas Finley
 */
public class FiniteStateAutomaton extends Automaton {

    /**
     * Creates a finite state automaton with no states and no transitions.
     */
    public FiniteStateAutomaton() {
        super();
    }

    /**
     * Returns the class of <CODE>Transition</CODE> this automaton must accept.
     *
     * @return the <CODE>Class</CODE> object for
     * <CODE>automata.fsa.FSATransition</CODE>
     */
    protected Class getTransitionClass() {
        return automata.fsa.FSATransition.class;
    }

    /*
     Modifications made to avoid problems when getting regexes for large transition's labels texts.
     */
    public FSATransition[] getFSATransitions() {
        FSATransition[] fsaTransitions;
        fsaTransitions = (FSATransition[]) this.transitions.toArray((FSATransition[]) Array.newInstance(getTransitionClass(), 0));
        return fsaTransitions;
    }

    Map<String, String> compressedLabelCodes;
    Map<String, String> labelsForCodes;

    SortedMap<BigInteger, String> charsInEncodedOrder;
    Iterator<BigInteger> currentCharCodeToUse;
    
    private void loadAvailableCodes() {
        charsInEncodedOrder = new TreeMap<BigInteger, String>();
        Charset charset = Charset.defaultCharset();
        final List<String> metaCharacters = Arrays.asList("<","(","[","{","\\","^","-","=","$","!","|","]","}",")","?","*","+",".",">",",");
        
        for (int i = Character.MIN_VALUE; i < Character.MAX_VALUE; i++) {
            String s = Character.toString((char) i);
            byte[] encoded = s.getBytes(charset);
            String decoded = new String(encoded, charset);
            if (s.equals(decoded) && !metaCharacters.contains(s)) {
                charsInEncodedOrder.put(new BigInteger(1, encoded), s);
            }
        }
        
        this.currentCharCodeToUse = this.charsInEncodedOrder.keySet().iterator();
    }

    private String getNextCode() {
        if (!currentCharCodeToUse.hasNext()) {
            throw new RuntimeException("No more characters available!");
        }

        BigInteger key = this.currentCharCodeToUse.next();
        return this.charsInEncodedOrder.get(key);
    }

    Map<FSATransition, FSATransition> compressedTransitionReplacements;
    
    public void compressTransitionLabelsAlphabet() {
        this.compressedLabelCodes = new HashMap<>();
        this.labelsForCodes = new HashMap<>();
        this.loadAvailableCodes();
        this.compressedTransitionReplacements = new HashMap<>();
        for (FSATransition fsaT : this.getFSATransitions()) {
            String label = fsaT.getLabel();

            if (!compressedLabelCodes.containsKey(label)) {
                String code = getNextCode();
                compressedLabelCodes.put(label, code);
                labelsForCodes.put(code, label);
            }
            
            FSATransition tr = new FSATransition(fsaT.getFromState(), fsaT.getToState(), compressedLabelCodes.get(label));
            compressedTransitionReplacements.put(tr, fsaT);
        }
        
        for (FSATransition tr : compressedTransitionReplacements.keySet()){
            this.replaceTransition(compressedTransitionReplacements.get(tr), tr);
        }
    }

    public void uncompressTransitionLabelsAlphabet() {

        if (labelsForCodes == null) {
            return;
        }

        for (FSATransition tr : compressedTransitionReplacements.keySet()){
            this.replaceTransition(tr,compressedTransitionReplacements.get(tr));
        }
    }


    public String decodeRegexFromCompressedAutomaton(String regex) {
        for (String code : this.labelsForCodes.keySet()){
            regex = regex.replaceAll(code, labelsForCodes.get(code));
        }
        
        return regex;
    }
}
