
var redraw, g, renderer;

/* only do all this when document has finished loading (needed for RaphaelJS) */
window.onload = function() {
    var width = $(document).width() - 20;
    var height = $(document).height() - 60;
    
    
    // crate a list of functionally meaningless properties
    var testArr2_0 = [];
    testArr2_0.push(new PropertyItem("dragon","reptile"));
    testArr2_0.push(new PropertyItem("fwefawe","reptile"));
    testArr2_0.push(new PropertyItem("velociraptor","avian"));
    testArr2_0[1].setIsReturned(true);
   
    var testArr2_1 = [];
    testArr2_1.push(new PropertyItem("dragon","reptile"));
    testArr2_1.push(new PropertyItem("igwefhhuana","reptile"));
    testArr2_1.push(new PropertyItem("velociraptor","avian"));
    testArr2_1[1].setIsReturned(true);
    
    var testArr2_2 = [];
    testArr2_2.push(new PropertyItem("dragon","reptile"));
    testArr2_2.push(new PropertyItem("rah","reptile"));
    testArr2_2.push(new PropertyItem("velociraptor","avian"));
    testArr2_2[1].setIsReturned(true);
    
    var testArr2_3 = [];
    testArr2_3.push(new PropertyItem("dragon","reptile"));
    testArr2_3.push(new PropertyItem("yjyjsrth","reptile"));
    testArr2_3.push(new PropertyItem("velociraptor","avian"));
    testArr2_3[1].setIsReturned(true);
    
    var testArr2_4 = [];
    testArr2_4.push(new PropertyItem("dragon","reptile"));
    testArr2_4.push(new PropertyItem("sthsergwe","reptile"));
    testArr2_4.push(new PropertyItem("velociraptor","avian"));
    testArr2_4[2].setIsReturned(true);
    
    // create a collection of Semantic Nodes
    
    var tester0 = new SemanticNode("tester_0", testArr2_0, []);
    var tester1 = new SemanticNode("tester_1", testArr2_1, []);
    var tester2 = new SemanticNode("tester_2", testArr2_2, []);
    var tester3 = new SemanticNode("tester_3", testArr2_3, []);
    var tester4 = new SemanticNode("tester_4", testArr2_4, []);
    

       
    // add the nodelists
    var testArr_NodeList2 = [];			// for tester2
    var t2_t3_conn = new NodeItem(tester3.getNodeName(), "a test");
    t2_t3_conn.setConnected(true);
    t2_t3_conn.setConnectBy("hasT2T3TestConnection");
    t2_t3_conn.setSNode(tester3);

    var t2_t4_conn = new NodeItem(tester4.getNodeName(), "a test");
    t2_t4_conn.setConnected(true);
    t2_t4_conn.setConnectBy("hasT2T4TestConnection");
    t2_t4_conn.setSNode(tester4);
    
    testArr_NodeList2.push(t2_t3_conn);
    testArr_NodeList2.push(t2_t4_conn);
    
    tester2.setNList(testArr_NodeList2);
    
    // node tester1
    var testArr_NodeList1 = [];			// for tester2
    
    var t1_t4_conn = new NodeItem(tester4.getNodeName(), "a test");
    t1_t4_conn.setConnected(true);
    t1_t4_conn.setConnectBy("hasT1T4TestConnection");
    t1_t4_conn.setSNode(tester4);
    
    testArr_NodeList1.push(t1_t4_conn);
    tester1.setNList(testArr_NodeList1);
    
    
    var newTestSemanticToy = new SemanticNodeGroup(800, 600);
    newTestSemanticToy.addNode(tester0);
    newTestSemanticToy.addNode(tester1);
    newTestSemanticToy.addNode(tester2);
    newTestSemanticToy.addNode(tester3);
    newTestSemanticToy.addNode(tester4);

    newTestSemanticToy.drawNodes();
    newTestSemanticToy.generateSparql();
};

