<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<graphml xmlns="http://graphml.graphdrawing.org/xmlns" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <key attr.name="originFileName" attr.type="string" for="edge" id="originfile">
    <default>&lt;command-line&gt;</default>
  </key>
  <key attr.name="invariant" attr.type="string" for="node" id="invariant"/>
  <key attr.name="invariant.scope" attr.type="string" for="node" id="invariant.scope"/>
  <key attr.name="isViolationNode" attr.type="boolean" for="node" id="violation">
    <default>false</default>
  </key>
  <key attr.name="isEntryNode" attr.type="boolean" for="node" id="entry">
    <default>false</default>
  </key>
  <key attr.name="isSinkNode" attr.type="boolean" for="node" id="sink">
    <default>false</default>
  </key>
  <key attr.name="enterLoopHead" attr.type="boolean" for="edge" id="enterLoopHead">
    <default>false</default>
  </key>
  <key attr.name="cyclehead" attr.type="boolean" for="edge" id="cyclehead">
    <default>false</default>
  </key>
  <key attr.name="threadId" attr.type="int" for="edge" id="threadId">
    <default>0</default>
  </key>
  <key attr.name="createThread" attr.type="int" for="edge" id="createThread">
    <default>0</default>
  </key>
  <key attr.name="sourcecodeLanguage" attr.type="string" for="graph" id="sourcecodelang"/>
  <key attr.name="programFile" attr.type="string" for="graph" id="programfile"/>
  <key attr.name="programHash" attr.type="string" for="graph" id="programhash"/>
  <key attr.name="specification" attr.type="string" for="graph" id="specification"/>
  <key attr.name="architecture" attr.type="string" for="graph" id="architecture"/>
  <key attr.name="producer" attr.type="string" for="graph" id="producer"/>
  <key attr.name="startline" attr.type="int" for="edge" id="startline"/>
  <key attr.name="control" attr.type="string" for="edge" id="control"/>
  <key attr.name="assumption" attr.type="string" for="edge" id="assumption"/>
  <key attr.name="assumption.resultfunction" attr.type="string" for="edge" id="assumption.resultfunction"/>
  <key attr.name="assumption.scope" attr.type="string" for="edge" id="assumption.scope"/>
  <key attr.name="enterFunction" attr.type="string" for="edge" id="enterFunction"/>
  <key attr.name="returnFromFunction" attr.type="string" for="edge" id="returnFrom"/>
  <key attr.name="witness-type" attr.type="string" for="graph" id="witness-type"/>
  <graph edgedefault="directed">
      <data key="witness-type">violation_witness</data>
      <data key="producer">GDart</data>
      <!-- data key="specification">na</data>
      <data key="programfile">na</data>
      <data key="programhash">na</data>
      <data key="architecture">64bit</data>
      <data key="sourcecodelang">Java</data -->
    <node id="sink"/>

   $nodes:{ n |
    <node id="n$n.id$">
    $n.data.keys:{k |
      <data key="$k$">$n.data.(k)$</data> }$
    </node>
   }$

   $edges:{ e |
    <edge source="n$e.source.id$" target="n$e.dest.id$">
      <data key="originfile">$e.witness.clazz$</data>
      <data key="startline">$e.witness.line$</data>
      <data key="threadId">0</data>
      <data key="assumption">$e.assumption$</data>
      <data key="assumption.scope">java::$e.witness.scope$</data>
    </edge> }$

  </graph>
</graphml>