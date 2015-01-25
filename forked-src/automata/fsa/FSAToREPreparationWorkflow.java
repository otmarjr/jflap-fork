/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automata.fsa;

import automata.State;
import automata.Transition;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import regular.RegularExpression;

/**
 *
 * @author Otmar
 */
public final class FSAToREPreparationWorkflow {

    Set<State> selectedStates;
    Set<Transition> selectedTransitions;

    private int getNumberOfSelectedStates() {
        return selectedStates.size();
    }

    private void clearSelected() {
        clearSelectedStates();
        clearSelectedTransitions();
    }

    private void clearSelectedStates() {
        selectedStates.clear();
    }

    private void addToSelectedStates(State state) {
        selectedStates.add(state);
    }

    private void addToSelectedTransitions(Transition transition) {
        selectedTransitions.add(transition);
    }

    private void clearSelectedTransitions() {
        this.selectedTransitions.clear();
    }

    private boolean isStateSelected(State state) {
        return selectedStates.contains(state);
    }

    private void removeSelectedState(State from) {
        this.selectedStates.remove(from);
    }

    private String mainStepMessage;
    private String detailStepMessage;

    private void setMainStepMessage(String text) {
        this.mainStepMessage = text;
    }

    public String getMainStepMessage() {
        return mainStepMessage;
    }

    private void setDetailStepMessage(String text) {
        this.detailStepMessage = text;
    }

    public String getDetailStepMessage() {
        return detailStepMessage;
    }

    /**
     * Instantiates a new <CODE>FSAToREController</CODE>.
     *
     * @param automaton the automaton that is in the process of being converted
     * @param detailStep the label holding the detail description of whatever
     * the user must do now
     * @param frame the window that this is all happening in
     */
    public FSAToREPreparationWorkflow(FiniteStateAutomaton automaton) {
        this.selectedStates = new HashSet<>();
        this.selectedTransitions = new HashSet<>();
        this.automaton = automaton;

        nextStep();
    }

    private void showMessageDialog(String message, String title) {
        throw new RuntimeException(title + " - " + message);
    }

    /**
     * This method should be called when the user undertakes an action that is
     * inappropriate for the current step. This merely displays a small dialog
     * to the user informing him of this fact, and takes no further action.
     */
    protected void outOfOrder() {
        showMessageDialog(
                "That action is inappropriate for this step!", "Out of Order");
    }

    /**
     * Moves the converter controller to the next step. This will skip any
     * unnecessary steps, and set the messages.
     */
    protected void nextStep() {
        switch (currentStep) {
            case -1:
            case CREATE_SINGLE_FINAL:
                currentStep = CREATE_SINGLE_FINAL;
                setMainStepMessage("Make Single Noninitial Final State");
                setDetailStepMessage("Create a new state to make a single final state.");
                if (automaton.getFinalStates().length != 1
                        || automaton.getFinalStates()[0] == automaton
                        .getInitialState()) {
                    return;
                }
                currentStep = TRANSITIONS_TO_SINGLE_FINAL;
            case TRANSITIONS_TO_SINGLE_FINAL:
                setDetailStepMessage("Put " + "lambda" + "-transitions from old final states to new.");
                // We know we're done when...
                if (getNumberOfSelectedStates() != 0) {
                    return;
                }
                currentStep = CONVERT_TRANSITIONS;
                remaining = collapsesNeeded();
            case CONVERT_TRANSITIONS:
                setMainStepMessage("Reform Transitions");
                setDetailStepMessage("Use the collapse tool to turn multiple transitions to one."
                        + " " + remaining + " more collapses needed.");
                if (remaining != 0) {
                    return;
                }
                currentStep = CREATE_EMPTY_TRANSITIONS;
                remaining = emptyNeeded();
            case CREATE_EMPTY_TRANSITIONS:
                setDetailStepMessage("Put empty transitions between states with no transitions."
                        + " "
                        + remaining
                        + " more empty transitions needed.");
                if (remaining != 0) {
                    return;
                }
                remaining = automaton.getStates().length - 2;
                currentStep = COLLAPSE_STATES;
            case COLLAPSE_STATES:
                setMainStepMessage("Remove States");
                setDetailStepMessage("Use the collapse state tool to remove nonfinal, noninitial "
                        + "states. " + remaining + " more removals needed.");
                if (remaining != 0) {
                    return;
                }
                clearSelected();
                clearSelectedTransitions();
                ;
                currentStep = FINISHED;
            case FINISHED:
                setMainStepMessage("Generalized Transition Graph Finished!");
                computedRE = FSAToRegularExpressionConverter.getExpressionFromGTG(automaton);
                setDetailStepMessage(computedRE);
        }
    }

    /**
     * For the collapsing of multiple transitions between states, this counts
     * the number of collapses that must take place on the automaton before all
     * possible ordered pairs of states have at most one transition from the
     * first to the second. This method just counts the number of
     * <CODE>(from,to)</CODE> pairs with more than one transition between them
     *
     * @return the number of collapses needed
     */
    protected int collapsesNeeded() {
        State[] states = automaton.getStates();
        int needed = 0;
        for (int i = 0; i < states.length; i++) {
            for (int j = 0; j < states.length; j++) {
                if (automaton.getTransitionsFromStateToState(states[i],
                        states[j]).length > 1) {
                    needed++;
                }
            }
        }
        return needed;
    }

    /**
     * For the creation of empty transitions between states, this counts the
     * number of empty transitions needed.
     *
     * @return the number of empty transitions needed
     */
    protected int emptyNeeded() {
        State[] states = automaton.getStates();
        int needed = 0;
        for (int i = 0; i < states.length; i++) {
            for (int j = 0; j < states.length; j++) {
                if (automaton.getTransitionsFromStateToState(states[i],
                        states[j]).length == 0) {
                    needed++;
                }
            }
        }
        return needed;
    }

    /**
     * Creates a new transition. There are two times when this would be
     * appropriate: first, when creating the labmda transitions from previously
     * final states to the new final state, and two, when creating the empty set
     * transitions between states that do not have transitions between
     * themselves already. Otherwise, this action should not be undertaken.
     * These transition creations do not require any user input since in either
     * case, what must go in the label is clear.
     *
     * @param from the from state
     * @param to the to state
     * @return the newly created transition from <CODE>from</CODE> to
     * </CODE>to</CODE>, or <CODE>null</CODE> if a transition is inappropriate
     * for this circumstance
     */
    public Transition transitionCreate(State from, State to) {
        if (currentStep == TRANSITIONS_TO_SINGLE_FINAL) {
            if (automaton.getFinalStates()[0] != to) {
                showMessageDialog("Transitions must go to the new final state!",
                        "Bad Destination");
                return null;
            }
            if (isStateSelected(from)) {
                showMessageDialog(
                        "Transitions must come from an old final state!",
                        "Bad Source");
                return null;
            }
            Transition t = new FSATransition(from, to, "");
            removeSelectedState(from);
            automaton.addTransition(t);
            if (getNumberOfSelectedStates() == 0) {
                nextStep();
            }
            return t;
        }
        if (currentStep == CREATE_EMPTY_TRANSITIONS) {
            if (automaton.getTransitionsFromStateToState(from, to).length != 0) {
                showMessageDialog(
                        "Transitions must go between"
                        + "states with no transitions!",
                        "Transition Already Exists");
                return null;
            }
            Transition t = FSAToRegularExpressionConverter.addTransitionOnEmptySet(from, to, automaton);
            remaining--;
            nextStep();
            return t;
        }
        outOfOrder();
        return null;
    }

    /**
     * This takes all the transitions from one state to another, and combines
     * them into a single transition.
     *
     * @param from the from state
     * @param to the to state
     * @return the newly created super transition that replaced all the
     * transitions that used to go from <CODE>from</CODE> to </CODE>to</CODE>,
     * or <CODE>null</CODE> if the transitions could not be collapsed (either
     * because there is already only one, or there are none, or if this isn't
     * the right time to collapse)
     */
    public Transition transitionCollapse(State from, State to) {
        if (currentStep != CONVERT_TRANSITIONS) {
            outOfOrder();
            return null;
        }
        Transition[] ts = automaton.getTransitionsFromStateToState(from, to);
        if (ts.length <= 1) {
            showMessageDialog(
                    "Collapse requires 2 or more transitions!",
                    "Too Few Transitions");
            return null;
        }
        Transition t = FSAToRegularExpressionConverter.combineToSingleTransition(from, to, ts,
                automaton);
        remaining--;
        nextStep();
        return t;
    }

    /**
     * This takes a state, and prepares to remove it. Note that this does not
     * actually remove the state, but notifies the user of what will appear.
     *
     * @param state the state that was selected for removal
     * @return <CODE>false</CODE> if this state cannot be removed because it is
     * initial or final or because this is the wrong time for this operation,
     * <CODE>true</CODE> otherwise
     */
    public boolean stateCollapse(State state) {
        if (currentStep != COLLAPSE_STATES) {
            outOfOrder();
            return false;
        }
        if (automaton.getInitialState() == state) {
            throw new RuntimeException("The initial state cannot be removed! Initial State Selected");
        }

        if (automaton.getFinalStates()[0] == state) {
            throw new RuntimeException("The final state cannot be removed! Final State Selected");
        }

        collapseState = state;
        clearSelectedStates();
        addToSelectedStates(collapseState);
        return true;
    }

    /**
     * This finalizes a state remove. This will remove whatever state was
     * selected.
     */
    public void finalizeStateRemove() {
        if (collapseState == null) {
            showMessageDialog(
                    "A valid state has not been selected yet!",
                    "No State Selected");
            return;
        }
        remaining--;
        nextStep();
        collapseState = null;
        clearSelectedStates();
        clearSelectedTransitions();;
        // transitionWindow.setTransitions(new Transition[0]);
        // transitionWindow.hide();
    }

    /**
     * If a transition is selected in the transition window, this method is told
     * about it.
     *
     * @param transition the transition that was selected, or <CODE>null</CODE>
     * if less or more than one transition is selected
     */
    public void tableTransitionSelected(Transition transition) {
        clearSelectedTransitions();;
        if (transition == null || collapseState == null) {
            return;
        }
        State from = transition.getFromState();
        State to = transition.getToState();
        Transition a = automaton.getTransitionsFromStateToState(from,
                collapseState)[0];
        Transition b = automaton.getTransitionsFromStateToState(from, to)[0];
        Transition c = automaton.getTransitionsFromStateToState(collapseState,
                collapseState)[0];
        Transition d = automaton.getTransitionsFromStateToState(collapseState,
                to)[0];

        this.addToSelectedTransitions(a);
        this.addToSelectedTransitions(b);
        this.addToSelectedTransitions(c);
        this.addToSelectedTransitions(d);
    }

    public boolean isDone() {
        return currentStep == FINISHED;
    }

    public FiniteStateAutomaton getPreaparedFSA() {
        if (!this.isDone()) {
            return null;
        }

        return this.automaton;
    }

    public void perform() {
        while (!isDone()) {
            moveNextStep();
        }
    }

    private void createSingleFinalState() {
        List<State> finalSates;
        finalSates = new LinkedList<>(Arrays.asList(automaton.getFinalStates()));

        finalSates.forEach(fs -> automaton.removeFinalState(fs));

        State newFinalState = automaton.createStateWithId(null, automaton.getStates().length);
        automaton.addFinalState(newFinalState);
        this.selectedStates.addAll(finalSates);
        currentStep = TRANSITIONS_TO_SINGLE_FINAL;
    }

    private static void collapseStates(FiniteStateAutomaton automaton, State s) {
        Transition[] t = FSAToRegularExpressionConverter.getTransitionsForRemoveState(s,
                automaton);
        FSAToRegularExpressionConverter.removeState(s, t, automaton);
    }

    /**
     * This will automatically perform the actions to move the conversion to the
     * next step.
     */
    private void moveNextStep() {
        switch (currentStep) {
            case CREATE_SINGLE_FINAL:
                createSingleFinalState();
                return;
            case TRANSITIONS_TO_SINGLE_FINAL:
                State[] states = new State[this.getNumberOfSelectedStates()];
                states = this.selectedStates.toArray(states);
                State finalState = automaton.getFinalStates()[0];
                for (int i = 0; i < states.length; i++) {
                    this.removeSelectedState(states[i]);
                    transitionCreate(states[i], finalState);
                }
                break;
            case CONVERT_TRANSITIONS: {
                State[] s = automaton.getStates();
                for (int i = 0; i < s.length; i++) {
                    for (int j = 0; j < s.length; j++) {
                        if (automaton.getTransitionsFromStateToState(s[i], s[j]).length > 1) {
                            transitionCollapse(s[i], s[j]);
                        }
                    }
                }
                break;
            }
            case CREATE_EMPTY_TRANSITIONS: {
                State[] s = automaton.getStates();
                for (int i = 0; i < s.length; i++) {
                    for (int j = 0; j < s.length; j++) {
                        if (automaton.getTransitionsFromStateToState(s[i], s[j]).length == 0) {
                            transitionCreate(s[i], s[j]);
                        }
                    }
                }
                break;
            }
            case COLLAPSE_STATES:
                State[] s = automaton.getStates();
                for (int i = 0; i < s.length; i++) {
                    if (automaton.getFinalStates()[0] == s[i]
                            || automaton.getInitialState() == s[i]) {
                        continue;
                    }
                    collapseStates(automaton, s[i]);
                    /*
                    Transition[] t = FSAToRegularExpressionConverter.getTransitionsForRemoveState(s[i],
                            automaton);
                    FSAToRegularExpressionConverter.removeState(s[i], t, automaton);
                    */
                }
                remaining = 0;
                nextStep();
                break;
            case FINISHED:
                showMessageDialog("You're done.  Go away.",
                        "You're Done!");
                return;
            default:
                showMessageDialog(
                        "This shouldn't happen!  Notify Thomas.",
                        "Uh Oh, I'm Stupid!");
        }
        // nextStep();
    }

    /**
     * This will export the regular expression.
     */
    public RegularExpression export() {
        if (computedRE == null) {
            showMessageDialog(
                    "The conversion has not yet finished.", "Not Finished");
            return null;
        }
        return new RegularExpression(computedRE);
    }

    /**
     * This will export the current automaton. Used for special purposes.
     */
    void exportAutomaton() {
    }

    /**
     * The current step of the conversion process.
     */
    private int currentStep = -1;

    /**
     * The automaton that's being converted.
     */
    private FiniteStateAutomaton automaton;

    /**
     * The number of things left to do. This can be used by different steps.
     */
    private int remaining = 0;

    /**
     * The state last selected for state collapsing.
     */
    private State collapseState = null;

    /**
     * The final answer, or null if not done.
     */
    private String computedRE = null;

    /**
     * The state IDs of each of the steps. Fine, this sucks. So sue me.
     */
    private static final int CREATE_SINGLE_FINAL = 0,
            TRANSITIONS_TO_SINGLE_FINAL = 1, CONVERT_TRANSITIONS = 2,
            CREATE_EMPTY_TRANSITIONS = 3, COLLAPSE_STATES = 4, FINISHED = 200;
}
