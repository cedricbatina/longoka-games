#target "InDesign"

(function () {
  var thisFile = File($.fileName);
  var forIndesignFolder = Folder(thisFile.parent.parent + "/for Indesign");
  $.global.LG_BASE_FOLDER = forIndesignFolder.fsName;
  $.evalFile(File(forIndesignFolder + "/Longoka_Arrowword_Book_FromPack.jsx"));
})();
