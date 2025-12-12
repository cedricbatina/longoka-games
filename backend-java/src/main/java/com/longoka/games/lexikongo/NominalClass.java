package com.longoka.games.lexikongo;

public class NominalClass {

 private final int classId;
 private final String className;

 public NominalClass(int classId, String className) {
  this.classId = classId;
  this.className = className;
 }

 public int getClassId() {
  return classId;
 }

 public String getClassName() {
  return className;
 }

 @Override
 public String toString() {
  return "NominalClass{" +
    "classId=" + classId +
    ", className='" + className + '\'' +
    '}';
 }
}
