package com.longoka.games.lexikongo;

public enum NumberVariability {
 SINGULAR_ONLY("singular_only"),
 PLURAL_ONLY("plural_only"),
 INVARIABLE("invariable"),
 VARIABLE("variable");

 private final String dbValue;

 NumberVariability(String dbValue) {
  this.dbValue = dbValue;
 }

 public String getDbValue() {
  return dbValue;
 }

 public static NumberVariability fromDbValue(String value) {
  if (value == null) {
   return VARIABLE;
  }
  switch (value) {
   case "singular_only":
    return SINGULAR_ONLY;
   case "plural_only":
    return PLURAL_ONLY;
   case "invariable":
    return INVARIABLE;
   case "variable":
   default:
    return VARIABLE;
  }
 }
}
