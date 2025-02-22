/*
 * Copyright (C) 2015, United States Government, as represented by the 
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 * 
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment 
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may obtain a 
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 * 
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package tools.aqua.dse.paths;

import gov.nasa.jpf.constraints.api.Valuation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/*
 * this file was originally part of the JDart concolic execution engine
 */
public class PathResult {

  private List<String> taintViolations = new LinkedList<>();

  public void setTaintViolations(List<String> taintViolations) {
    this.taintViolations = taintViolations;
  }

  public List<String> getTaintViolations() {
    return taintViolations;
  }

  public static abstract class ValuationResult extends PathResult {
    private final Valuation valuation;
    
    private ValuationResult(PathState ps, Valuation valuation) {
      super(ps);
      this.valuation = valuation;
    }
    
    public Valuation getValuation() {
      return valuation;
    }

    public void print(Appendable a, boolean printDetails, boolean printValues) throws IOException {
      super.print(a, printValues, printDetails);
      if(printValues) {
        a.append(": ");
        valuation.print(a);
      }
    }
  }
  
  public static final class OkResult extends ValuationResult {

    public OkResult(Valuation valuation) {
      super(PathState.OK, valuation);
    }
    
    public void print(Appendable a, boolean printPost, boolean printValues) throws IOException {
      super.print(a, printPost, printValues);
      if(printPost) {
        a.append((printValues) ? ", " : ": ");
      }
    }
    
  }

  public static final class AbortResult extends ValuationResult {
    private final String reason;

    public AbortResult(Valuation valuation, String reason) {
      super(PathState.ABORT, valuation);
      this.reason = reason;
    }

    public String getReason() {
      return reason;
    }

    public void print(Appendable a, boolean printExcName, boolean printValues) throws IOException {
      super.print(a, printExcName, printValues);
      if(printExcName) {
        a.append((printValues) ? ", " : ": ");
        a.append(reason);
      }
    }
  }


  public static final class ErrorResult extends ValuationResult {
    private final String exceptionClass;
    private final String stackTrace;
    
    public ErrorResult(Valuation valuation, String exceptionClass, String stackTrace) {
      super(PathState.ERROR, valuation);
      this.exceptionClass = exceptionClass;
      this.stackTrace = stackTrace;
    }
    
    public String getExceptionClass() {
      return exceptionClass;
    }
    
    public String getStackTrace() {
      return stackTrace;
    }
    
    public void print(Appendable a, boolean printExcName, boolean printValues) throws IOException {
      super.print(a, printExcName, printValues);
      if(printExcName) {
        a.append((printValues) ? ", " : ": ");
        a.append(exceptionClass);
      }
    }
  }
  
  public static OkResult ok(Valuation valuation) {
    return new OkResult(valuation);
  }

  public static AbortResult abort(Valuation valuation, String reason) {
    return new AbortResult(valuation, reason);
  }

  public static ErrorResult error(Valuation valuation, String exceptionClass, String stackTrace) {
    return new ErrorResult(valuation, exceptionClass, stackTrace);
  }
  
  public static PathResult dontKnow() {
    return DONT_KNOW;
  }

  public static PathResult DONT_KNOW = new PathResult(PathState.DONT_KNOW);

  private final PathState state;
  
  protected PathResult(PathState state) {
    this.state = state;
  }
  
  public PathState getState() {
    return state;
  }
  
  public void print(Appendable a, boolean printDetails, boolean printValues) throws IOException {
    // TODO: maybe print taint?
    a.append(state.toString());
  }
  
  @Override
  public String toString() {
    return toString(true, false);
  }
  
  public String toString(boolean printDetails, boolean printValues) {
    StringBuilder sb = new StringBuilder();
    try {
      print(sb, printDetails, printValues);
      return sb.toString();
    }
    catch(IOException ex) {
      throw new IllegalStateException();
    }
  }

}
