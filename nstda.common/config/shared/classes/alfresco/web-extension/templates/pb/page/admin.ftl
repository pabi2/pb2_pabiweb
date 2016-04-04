<#include "/org/alfresco/include/alfresco-template.ftl" />

<@templateHeader>
   <@markup id="resizer">
	
   <script type="text/javascript">//<![CDATA[
      new Alfresco.widget.Resizer("Admin");
   //]]></script>
   </@>
   <@templateHtmlEditorAssets />  
</@>

<@templateBody>
   <@markup id="alf-hd">
   <div id="alf-hd">
      <@region scope="global" id="share-header" chromeless="true"/>
   </div>
   </@>
   <@markup id="bd">
   <div id="bd">
      <@region id="adminPage" scope="template" />
   </div>
   </@>
</@>
