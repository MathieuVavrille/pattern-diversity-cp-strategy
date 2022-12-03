package org.mvavrill.miningDiv.mining.models;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;

public class ReverseOrder<V extends Variable> implements VariableSelector<V> {

    private final IStateInt lastIdx; // index of the last non-instantiated variable

    public ReverseOrder(Model model){
        lastIdx = model.getEnvironment().makeInt(-1);
    }

    @Override
    public V getVariable(V[] variables) {
      if (lastIdx.get() == -1)
        lastIdx.set(variables.length-1);
        for (int idx = lastIdx.get(); idx >= 0; idx--) {
            if (!variables[idx].isInstantiated()) {
                lastIdx.set(idx);
                return variables[idx];
            }
        }
        lastIdx.set(-1);
        return null;
    }
}
