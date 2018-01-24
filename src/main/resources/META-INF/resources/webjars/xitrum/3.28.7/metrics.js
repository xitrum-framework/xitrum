(function($) {
   $.fn.fixHeader = function() {
      return this.each(function() {
        var $this   = $(this),
            $header = $this.children(".metricsTableHeader"),
            $body   = $this.children(".metricsTable")
            ;
        $($body.children('tbody').children()[0]).children().each(function(i, el){
          var h = $($header.find("th")[i]);
          var b = $(el);
          if (h.width() > b.width()) b.width(h.width());
          if (h.width() < b.width()) h.width(b.width());
        });
      });
   };
})(jQuery);


(function(){
  $(function(){
    var tooltip = d3.select("body")
                    .append("div")
                      .style("position", "absolute")
                      .style("z-index", "10")
                      .style("visibility", "hidden")
                      .style("background-color", "#DDDDDD")
                      .style("font-size", "12px")
                      .text("a simple tooltip")
                      ;

    var margin  = {top: 10, right: 20, bottom: 10, left: 20},
        width   = 600 - margin.left - margin.right,
        height  = 300 - margin.top - margin.bottom
        ;

    var $cpuTable        = $("#cpuTable"),
        $heapMemoryTable = $("#heapMemoryTable")
        ;

    var renderHeapMemoryGraph = (function(){
      var color = d3.scale.category10(),
          x     = d3.time.scale()
                    .range([0, width])
                    .nice(),
          y     = d3.scale.linear()
                    .range([height, 0])
                    .nice()
                    ;

      var xAxis = d3.svg.axis()
                    .scale(x)
                    .orient("bottom"),
          yAxis = d3.svg.axis()
                    .scale(y)
                    .orient("left")
                    .ticks(10),
          line  = d3.svg.line()
                        .interpolate("basis")
                        .x(function(d) { return x(d.date); })
                        .y(function(d) { return y(d.mbyte); })
                        ;

      var svg = d3.select("#heapMemoryGraph")
                  .append("svg")
                    .attr("width",  width  + margin.left + margin.right)
                    .attr("height", height + margin.top  + margin.bottom)
                    .append("g")
                      .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

      svg.append("g")
          .attr("class", "x axis")
          .attr("transform", "translate(30," + height + ")")
          .call(xAxis)
          .selectAll("text").remove();

      svg.append("g")
          .attr("class", "y axis")
          .attr("transform", "translate(30,0)")
          .call(yAxis)
          .append("text")
            .attr("y", 6)
            .attr("dx", "2em")
            .style("text-anchor", "end")
            .text("MB");

      var datasetByKeyNode = {};
      return function(data){
        d3.keys(data)
          .filter(function(key) { return key === "MAX" || key === "USED" || key === "COMMITTED"; })
          .forEach(function(key){
            var name   = key+"@"+data.node;
            var datas  = datasetByKeyNode[name] || {};
            datas.name = name;

            var values  = datas.values || [];
            values.push({date: new Date(data.TIMESTAMP), mbyte: + Math.floor(data[key]/ 1024 / 1024 * 100) / 100});
            datas.values = values;

            datasetByKeyNode[name] = datas;
          });

        var dataset = d3.values(datasetByKeyNode);

        color.domain(dataset);
        x.domain([
          d3.min(dataset, function(c) { return d3.min(c.values, function(v) { return v.date; }); }),
          d3.max(dataset, function(c) { return d3.max(c.values, function(v) { return v.date; }); })
        ]);
        y.domain([
          0,
          d3.max(dataset, function(c) { return d3.max(c.values, function(v) { return v.mbyte +100; }); })
        ]);

        yAxis = d3.svg.axis()
                  .scale(y)
                  .orient("left")
                  ;

        svg.selectAll(".y.axis")
            .call(yAxis)
            ;

        var heapGraph = svg.selectAll(".heap").data(dataset);
        var heap = heapGraph.enter()
                            .append("g")
                              .attr("class", "heap")
                              .attr("transform", "translate(30,0)")
                              ;

            heap.append("path")
                .attr("class", "line")
                ;

        svg.selectAll(".line")
           .attr("d", function(d) { return line(d.values); })
           .style("stroke", function(d) { return color(d.name); })
           .on("mouseover", function(){
             return tooltip.style("visibility", "visible");
           })
           .on("mousemove", function(d){
             return tooltip
                      .style("top",  (d3.event.pageY - 10) + "px")
                      .style("left", (d3.event.pageX + 10) + "px")
                      .html("<span>" + d.name + "</span>");
           })
           .on("mouseout", function(){
             return tooltip.style("visibility", "hidden");
           })
           ;

        heapGraph.exit().remove();
      };
    })();

    var renderLoadAverageGraph = (function(){

      var color = d3.scale.category10(),
          x     = d3.time.scale()
                    .range([0, width])
                    .nice(),
          y     = d3.scale.linear()
                    .range([height, 0])
                    .nice()
                    ;

      var xAxis = d3.svg.axis()
                    .scale(x)
                    .orient("bottom"),
          yAxis = d3.svg.axis()
                    .scale(y)
                    .orient("left")
                    .ticks(10),
          line  = d3.svg.line()
                        .interpolate("basis")
                        .x(function(d) { return x(d.date); })
                        .y(function(d) { return y(d.loadavarage); })
                        ;


      var svg = d3.select("#cpuGraph")
                  .append("svg")
                  .attr("width",  width  + margin.left + margin.right)
                  .attr("height", height + margin.top  + margin.bottom)
                  .append("g")
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");


      svg.append("g")
          .attr("class", "x axis")
          .attr("transform", "translate(10," + height + ")")
          .call(xAxis)
          .selectAll("text").remove();

      svg.append("g")
          .attr("class", "y axis")
          .attr("transform", "translate(10,0)")
          .call(yAxis)
          .append("text")
            .attr("y", 6)
            .attr("dx", "2em")
            .style("text-anchor", "start")
            .text("System load average");

      var datasetByKeyNode = {};
      return function(data){
        var d      = {date: new Date(data.TIMESTAMP), loadavarage: +data.SYSTEMLOADAVERAGE};
        var datas  = datasetByKeyNode[data.node] || {};
        datas.name = data.node;

        var values  = datas.values || [];
        values.push(d);

        datas.values = values;
        datasetByKeyNode[data.node] = datas;

        var dataset = d3.values(datasetByKeyNode);

        color.domain(dataset);
        x.domain([
          d3.min(dataset, function(c) { return d3.min(c.values, function(v) { return v.date; }); }),
          d3.max(dataset, function(c) { return d3.max(c.values, function(v) { return v.date; }); })
        ]);
        y.domain([
          0,
          d3.max(dataset, function(c) { return d3.max(c.values, function(v) { return v.loadavarage + 5; }); })
        ]);

        yAxis = d3.svg.axis()
                  .scale(y)
                  .orient("left")
                  .ticks(10)
                  ;

        svg.selectAll(".y.axis")
            .call(yAxis)
            ;

        var cpuGraph = svg.selectAll(".cpu").data(dataset);
        var cpu = cpuGraph.enter()
                          .append("g")
                            .attr("class", "cpu")
                            .attr("transform", "translate(10,0)")
                            ;

            cpu.append("path")
                .attr("class", "line")
                ;

        svg.selectAll(".line")
           .attr("d", function(d) {return line(d.values); })
           .style("stroke", function(d) { return color(d.name); })
           .on("mouseover", function(){
             return tooltip.style("visibility", "visible");
           })
           .on("mousemove", function(d){
             return tooltip
                      .style("top",  (d3.event.pageY - 10) + "px")
                      .style("left", (d3.event.pageX + 10) + "px")
                      .html("<span>" + d.name + "</span>");
           })
           .on("mouseout", function(){
             return tooltip.style("visibility", "hidden");
           })
           ;

        cpuGraph.exit().remove();
      };
    })();

    var renderActionCount = (function(){

      var x = d3.scale.ordinal()
                .rangeRoundBands([0, width], .1, 1),
          y = d3.scale.linear()
                .range([height, 0])
                .nice()
                ;

      var xAxis = d3.svg.axis()
                    .scale(x)
                    .orient("bottom"),
          yAxis = d3.svg.axis()
                    .scale(y)
                    .orient("left")
                    ;

      var svg = d3.select("#histogramsGraph")
                  .append("svg")
                  .attr("width",  width  + margin.left + margin.right)
                  .attr("height", height + margin.top  + margin.bottom)
                  .append("g")
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

      svg.append("g")
          .attr("class", "x axis")
          .attr("transform", "translate(10," + height + ")")
          .call(xAxis)
          .selectAll("text").remove();

      svg.append("g")
          .attr("class", "y axis")
          .attr("transform", "translate(10,0)")
          .call(yAxis)
          .append("text")
            .attr("y", 6)
            .attr("dx", "2em")
            .style("text-anchor", "start")
            .text("Action Execute Count");

      var datasetByKeyNode = {};
      return function(histograms, address){
        d3.keys(histograms)
          .forEach(function(key){
            var d = {name:key + "@" + address, count: histograms[key].count};
            datasetByKeyNode[key + "@" + address] = d;
          });

        var dataset = d3.values(datasetByKeyNode);

        x.domain(d3.keys(datasetByKeyNode));
        y.domain([
          0,
          d3.max(dataset, function(c) { return c.count + 20; })
        ]);


        yAxis = d3.svg.axis()
                  .scale(y)
                  .orient("left")
                  ;
        svg.selectAll(".y.axis")
          .call(yAxis)
          ;


        svg.selectAll(".histogram").remove();

        var histogramChart = svg.selectAll(".histogram")
                                .data(dataset)
                                ;

        var rects = histogramChart.enter()
                                  .append("g")
                                  .attr("class", "histogram")
                                  ;

            rects.append("rect")
                  .attr("class", "bar")
                  .attr("x", function(d) { return x(d.name); })
                  .attr("y", function(d) { return y(d.count); })
                  .attr("width", x.rangeBand())
                  .attr("height", function(d) { return height - y(d.count); })
                  .on("mouseover", function(){
                    return tooltip.style("visibility", "visible");
                  })
                  .on("mousemove", function(d){
                    return tooltip
                             .style("top",  (d3.event.pageY - 10) + "px")
                             .style("left", (d3.event.pageX + 10) + "px")
                             .html("<span>" + d.name + "</span>");
                  })
                  .on("mouseout", function(){
                    return tooltip.style("visibility", "hidden");
                  })
                  ;

            rects.append("text")
                  .attr("x", function(d) { return x(d.name) + x.rangeBand()/2; })
                  .attr("y", function(d) { return y(d.count); })
                  .style("text-anchor", "middle")
                  .style("font-size", "10px")
                  .text(function(d) { return d.count; })
                  ;
      };
    })();

    var renderMetricsWithKey = (function(){

    var margin  = {top: 10, right: 20, bottom: 10, left: 20},
        width   = 900 - margin.left - margin.right,
        height  = 300 - margin.top - margin.bottom
        ;

      var x     = d3.time.scale()
                    .range([0, width])
                    .nice(),
          y     = d3.scale.linear()
                    .range([height, 0])
                    .nice()
                    ;

      var xAxis = d3.svg.axis()
                    .scale(x)
                    .orient("bottom"),
          yAxis = d3.svg.axis()
                    .scale(y)
                    .orient("left")
                    .ticks(10)
                    ;

      line  = d3.svg.line()
                    .interpolate("linear")
                    .x(function(d) { return x(d.date); })
                    .y(function(d) { return y(d.ms); })
                    ;

      var svg = d3.select("#focusGraph")
                  .append("svg")
                  .attr("width",  width  + margin.left + margin.right)
                  .attr("height", height + margin.top  + margin.bottom)
                  .append("g")
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

      svg.append("g")
          .attr("class", "x axis")
          .attr("transform", "translate(20," + height + ")")
          .call(xAxis)
          .selectAll("text").remove();

      svg.append("g")
          .attr("class", "y axis")
          .attr("transform", "translate(20,0)")
          .call(yAxis)
          .append("text")
            .attr("y", 6)
            .attr("dx", "2em")
            .style("text-anchor", "start")
            .text("Execute time (milliseconds)")
            ;

      svg.append("path")
            .attr("class", "line")
            ;

      var datasetByDate = {};
      return function(registryJson, key){
        var gauges            = registryJson.gauges || {},
            lastExecTimeGauge = gauges["xitrum.anon.lastExecutionTime"].value;

        lastExecTimeGauge.forEach(function(obj){
          if (obj["_1"] === key.replace("xitrum.anon.", "")) {
            var keyDate = new Date(obj["_2"][0]).getTime();
            if (!datasetByDate[key]) datasetByDate[keyDate] = {date: new Date(obj["_2"][0]), ms:obj["_2"][1]};
          }
        });
        var dataset = d3.values(datasetByDate);
        x.domain([
          d3.min(dataset, function(d) { return d.date; }),
          d3.max(dataset, function(d) { return d.date; })
        ]);
        y.domain([
          0,
          d3.max(dataset, function(d) { return d.ms + 500; })
        ]);

        yAxis = d3.svg.axis()
                  .scale(y)
                  .orient("left")
                  ;

        svg.selectAll(".y.axis")
            .call(yAxis)
            ;

        svg.selectAll(".action").remove();
        var actions = svg.selectAll(".action")
           .data(dataset)
           ;

        var action = actions.enter()
                            .append("g")
                            .attr("class", "action")
                            ;

        action.append("circle")
              .attr("class", "dot")
              .attr("r", 5)
              .attr("cx", function(d) { return x(d.date); })
              .attr("cy", function(d) { return y(d.ms); })
              .attr("transform", "translate(20,0)")
              .style("fill", function(d) { return "steelblue"; })
              .on("mouseover", function(){
                return tooltip.style("visibility", "visible");
              })
              .on("mousemove", function(d){
                return tooltip
                         .style("top",  (d3.event.pageY - 10) + "px")
                         .style("left", (d3.event.pageX + 10) + "px")
                         .html("<span>" + formatDate(d.date) + " " + d.ms + "[ms]</span>");
              })
              .on("mouseout", function(){
                return tooltip.style("visibility", "hidden");
              })
              ;

        line  = d3.svg.line()
                      .interpolate("linear")
                      .x(function(d) { return x(d.date); })
                      .y(function(d) { return y(d.ms); })
                      ;

        svg.selectAll(".line")
           .attr("transform", "translate(20,0)")
           .attr("d", function(d) {return line(dataset); })
           .style("stroke", function(d) { return "#ff7f0e"; })
           ;

      };
    })();

    function formatDate(timestamp){
      var pad = function(s){ return ((""+s).length ===1) ? "0" + s + "" : s;},
          d   = new Date(timestamp)
          ;
      return d.getFullYear() + "/" + pad(d.getMonth()+1) + "/" + pad(d.getDate()) + " " + pad(d.getHours()) + ":" + pad(d.getMinutes()) + ":" + pad(d.getSeconds());
    }

    function renderHeapMemory(data) {
      var timestamp = data.TIMESTAMP,
          system    = data.SYSTEM,
          host      = data.HOST || "",
          port      = data.PORT || "",
          hash      = data.HASH,
          committed = Math.floor(data.COMMITTED / 1024 / 1024 * 100) / 100,
          used      = Math.floor(data.USED / 1024 / 1024 * 100) / 100,
          max       = Math.floor(data.MAX / 1024 / 1024 * 100) / 100,
          node      = system + "@" + host + ":" + port + "#" + hash
          ;
      data.node = node;
      $heapMemoryTable.append("<tr><td>"+formatDate(timestamp)+"</td><td>"+node+"</td><td>"+committed+"</td><td>"+used+"</td><td>"+max+"</td></tr>");
      $("#heapMemory").fixHeader();
      visualizeHeapMemory(data);
    }

    function renderCPU(data) {
      var timestamp         = data.TIMESTAMP,
          system            = data.SYSTEM,
          host              = data.HOST || "",
          port              = data.PORT || "",
          hash              = data.HASH,
          processors        = data.PROCESSORS,
          systemLoadAverage = Math.floor(data.SYSTEMLOADAVERAGE * 100) / 100,
          node              = system + "@" + host + ":" + port + "#" + hash
          ;
      data.node = node;

      $cpuTable.append("<tr><td>"+formatDate(timestamp)+"</td><td>"+node+"</td><td>"+processors+"</td><td>"+systemLoadAverage+"</td></tr>");
      $("#cpu").fixHeader();
      visualizeCpu(data);
    }

    function renderMetrics(data){
      var address    = data.address    || "",
          counters   = data.counters   || {},
          gauges     = data.gauges     || {},
          histograms = data.histograms || {},
          meters     = data.meters     || {},
          timers     = data.timers     || {}
          ;
      function appendStr(data, parent) {
        $.each(data, function(key, val){
          var d = data[key];
          // remove `[$./:@]` for jquery selector
          var domId = (key+address).replace(/[$./:@]/g,"_");
          if ($("#"+domId).length < 1) parent.append("<tr id='"+domId+"'>");
          $("#"+domId).html("<td>"+address+"</td>" +
                            "<td><a href='#' class='histogramKey' data-key='"+key+"'>"+key.replace("xitrum.package.anon.", "")+"</a></td>" +
                            "<td>"+d.count+"</td>" +
                            "<td>"+Math.floor(d.min * 100) / 100+"</td>" +
                            "<td>"+Math.floor(d.max * 100) / 100+"</td>" +
                            "<td>"+Math.floor(d.mean * 100) / 100+"</td>"
                            );
        });
      }
      appendStr(histograms,$("#histogramsTable"));
      $(".histogramKey").off().on("click", function(e){
        e.preventDefault(e);
        window.open(location.href+'&'+"focusAction="+$(e.currentTarget).data("key"));
      });
      $("#histograms").fixHeader();
      visualizeHistograms(histograms, address);
    }

    function visualizeHeapMemory(data) {
      data.node = data.SYSTEM+"://"+data.HOST+":"+data.PORT+"#"+data.HASH;
      renderHeapMemoryGraph(data);
    }
    function visualizeCpu(data) {
      data.node = data.SYSTEM+"://"+data.HOST+":"+data.PORT+"#"+data.HASH;
      renderLoadAverageGraph(data);
    }
    function visualizeHistograms(data, address) {
      renderActionCount(data, address);
    }

    var channelOnMessage = function(data) {
      var jsonObj = {};
      try {
        jsonObj = JSON.parse(data);
      } catch (e) {
        console.log("Parse Error",data);
        console.log(e);
      }
      switch (jsonObj.TYPE) {
        case "heapMemory":
          renderHeapMemory(jsonObj);
        break;
        case "cpu":
          renderCPU(jsonObj);
        break;
        case "metrics":
          renderMetrics(jsonObj);
        break;
        default :
      }
    };
    var channelOnMessageWithKey = function(key){
      $("#title").text(key.replace("xitrum.package.anon.", ""));
      return function(data) {
        var jsonObj = {};
        try {
          jsonObj = JSON.parse(data);
        } catch (e) {
          console.log("Parse Error",data);
          console.log(e);
        }
        switch (jsonObj.TYPE) {
          case "metrics":
            renderMetricsWithKey(jsonObj, key);
          break;
          default :
        }
      };
    };
    window.channelOnMessage        = channelOnMessage;
    window.channelOnMessageWithKey = channelOnMessageWithKey;
  });
})(window);