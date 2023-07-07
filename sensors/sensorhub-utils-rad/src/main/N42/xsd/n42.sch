<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2"
        fpi="ISO//ANSI N42 Committee//ANSI N42.42/IEC 62755 Base Data Validation Rules//EN">
  
  <title>ANSI N42.42-2011/IEC 62755 Data Format Base Data Validation Rules</title>
  
  <!--
  Description:
    This schema contains Schematron rules for validation of ANSI N42.42-2011 and IEC 62755 compliant documents.
    These rules are designed to be used in concert with XML Schema validation; they do not duplicate or replace
    the XML Schema rules. The rules contained in this schema are considered "universal" because they apply to
    any compliant "N42" format document.  Additional Schematron rules for validation of N42 documents in
    specific circumstances are contained in separate schemas.
    
    The Schematron rules in this document are derived from Annex M of the ANSI/IEEE N42.42-2011 and IEC 67455
    standards.
    
  Design notes:
    - Each rule in Annex M is represented by one Schematron pattern (with one exception; see below).
    - Where rule reuse is given or likely, an abstract pattern is defined; specific Annex M rules instantiate the abstract pattern with 
      appropriate arguments.
    - The abstract patterns are defined at the beginning of the schema, in alphabetical order.
    - Instantiated and directly defined patterns are appearing after the abstract patterns, also in alphabetical order.  The id of each 
      pattern matches the name of the Annex M rule.
    - The exception to the "one pattern per Annex M rule" is RadMeasurementUniqueDetectorCheck; there is one pattern per child 
      element mentioned in the Annex M rule.
    - In general, the assert text is taken straight from Annex M and should adequately describe the purpose of the test.
      Abstract patterns are briefly described in an XML comment at the head of the pattern.

  Revision history:
    January 2012: Original created by Bob Huckins
  -->

  <ns uri="http://physics.nist.gov/N42/2011/N42" prefix="n42"/>

  <!-- ************************ Abstract pattern definitions ************************ -->

  <pattern id="AbstractAttributeFutureDateCheck" abstract="true">
    <!-- Verifies that the date of the specified attribute is not in the future.  Parameter is attribute-name. -->
    <rule context="@$attribute-name">
      <assert test=". &lt; current-dateTime()">The value of the <value-of select="'$attribute-name'"/> attribute (<value-of select="."/>) cannot be in the future.</assert>
    </rule>
  </pattern>

  <pattern id="AbstractAttributeElementCheck" abstract="true">
    <!-- Verifies that if the specified attribute is specified, the element using that attribute has a specified child element. Parameters are element-name and attribute-name. -->
    <rule context="@$attribute-name">
      <assert test="../n42:$element-name">An element using the <value-of select="'$attribute-name'"/> attribute shall contain an <value-of select="'$element-name'"/> element.</assert>
    </rule>
  </pattern>

  <pattern id="AbstractAttributeParentCheck" abstract="true">
    <!-- Verifies that any element using the specified attribute is a child of the specified element.  Parameters are element-name and attribute-name. -->
    <rule context="*[*/@$attribute-name]">
      <assert test="local-name(.) = '$element-name'">An element using the <value-of select="'$attribute-name'"/> attribute shall be the decendant of a <value-of select="'$element-name'"/> element.</assert>
    </rule>
  </pattern>

  <pattern id="AbstractElementReferenceCardinalityCheck" abstract="true">
    <!-- Verifies that within a parent element, only one instance of the specified child element has the specified attribute with the same value.  Parameters are parentelement-name, childelement-name, attribute-name, and reference-type (used only in the assert message). -->
    <rule context="n42:$parentelement-name/n42:$childelement-name">
      <let name="attribute-value" value="string(@$attribute-name)"/>
      <assert test="count(../n42:$childelement-name[@$attribute-name=$attribute-value]) = 1">In a <value-of select="'$parentelement-name'"/> element, there shall be only one <value-of select="'$childelement-name'"/> element which references the same <value-of select="'$reference-type'"/> (<value-of select="./@$attribute-name"/>).</assert>
    </rule>
  </pattern>

  <pattern id="AbstractFutureDateCheck" abstract="true">
    <!-- Verifies that the date of the specified element is not in the future.  Parameter is element-name. -->
    <rule context="n42:$element-name">
      <assert test=". &lt; current-dateTime()">The value of the <value-of select="'$element-name'"/> element (<value-of select="."/>) cannot be in the future.</assert>
    </rule>
  </pattern>

  <pattern id="AbstractOrderedElementCheck" abstract="true">
    <!-- Verifies that the values in a list element appear in strictly increasing order.  Parameter is element-name. -->
    <rule context="n42:$element-name">
      <let name="list-as-seq" value="tokenize(normalize-space(string(.)),'\s+')"/>
      <let name="list-positions" value="2 to count(tokenize(normalize-space(string(.)),'\s+'))"/>
      <!-- The test uses a for expression to check that each value is less than it's predecessor; if there are any falses in the returned sequence, index-of() will find them and produce a non-empty sequence, which will cause empty() to return false -->
      <assert test="empty(index-of(for $x in $list-positions return number($list-as-seq[$x - 1]) &lt; number($list-as-seq[$x]),false()))">The values of a <value-of select="'$element-name'"/> element shall appear in ascending order (<value-of select="."/>)."/></assert>
    </rule>
  </pattern>

  <pattern id="AbstractNonDecreasingElementCheck" abstract="true">
    <!-- Verifies that the values in a list element appear in order (greater than or equal to).  Parameter is element-name. -->
    <rule context="n42:$element-name">
      <let name="list-as-seq" value="tokenize(normalize-space(string(.)),'\s+')"/>
      <let name="list-positions" value="2 to count(tokenize(normalize-space(string(.)),'\s+'))"/>
      <!-- The test uses a for expression to check that each value is less than it's predecessor; if there are any falses in the returned sequence, index-of() will find them and produce a non-empty sequence, which will cause empty() to return false -->
      <assert test="empty(index-of(for $x in $list-positions return not(number($list-as-seq[$x - 1]) &gt; number($list-as-seq[$x])),false()))">The values of a <value-of select="'$element-name'"/> element shall appear in ascending order (<value-of select="."/>)."/></assert>
    </rule>
  </pattern>
  
  <pattern id="AbstractOtherDescriptionCheck" abstract="true">
    <!-- Verifies that if the value of the specified element is 'Other', there is another specified element as a child of the same parent.  Parameters are parentelement-name, childelement-name, and descriptionelement-name. -->
    <rule context="n42:$parentelement-name[n42:$childelement-name='Other']">
      <assert test="n42:$descriptionelement-name">A <value-of select="'$parentelement-name'"/> element containing a <value-of select="'$childelement-name'"/> element with a value of 'Other' shall also contain the <value-of select="'$descriptionelement-name'"/> element.</assert>
    </rule>
  </pattern>

  <pattern id="AbstractPairedElementOrderCheck" abstract="true">
    <!-- Verifies that the values in a list element are less than the corresponding values in a specified sibling element.  Parameters are parent-name, element1-name, and element2-name. -->
    <rule context="n42:$parent-name">
      <let name="list1-as-seq" value="tokenize(normalize-space(string(n42:$element1-name)),'\s+')"/>
      <let name="list2-as-seq" value="tokenize(normalize-space(string(n42:$element2-name)),'\s+')"/>
      <let name="list-positions" value="1 to count($list1-as-seq)"/>
      <!-- The test uses a for expression to check that each value in element1 is less than it's partner in element2; if there are any falses in the returned sequence, index-of() will find them and produce a non-empty sequence, which will cause empty() to return false-->
      <assert test="empty(index-of(for $x in $list-positions return number($list1-as-seq[$x]) &lt; number($list2-as-seq[$x]),false()))">Each value of a <value-of select="'$element1-name'"/> element shall less than the corresponding value in the sibling <value-of select="'$element2-name'"/> element.</assert>
    </rule>
  </pattern>

  <pattern id="AbstractReferencesTypeMatch" abstract="true">
    <!-- Verifies that all values of the specified attribute are the id of an instance of the specified element.  Parameters are element-name and attribute-name. -->
    <rule context="@$attribute-name">
      <!-- The test uses a for expression to check that each value points to an element of the right type; if there are any falses in the returned sequence, index-of() will find them and produce a non-empty sequence, which will cause empty() to return false-->
      <assert test="empty(index-of(for $x in tokenize(normalize-space(string(.)),'\s+') return local-name(id($x)) = '$element-name',false()))">Each value of the <value-of select="'$attribute-name'"/> attribute (data type IDREFS) shall be the id of a <value-of select="'$element-name'"/> element.</assert>
    </rule>
  </pattern>

  <pattern id="AbstractReferenceTypeMatch" abstract="true">
    <!-- Verifies that the value of the specified attribute are the id of an instance of the specified element.  Parameters are element-name and attribute-name. -->
    <!-- This pattern really could be subsumed into the "references" pattern, but I'm too tired to consolidate them -->
    <rule context="@$attribute-name">
      <assert test="local-name(id(.)) = '$element-name'">The value of an <value-of select="'$attribute-name'"/> attribute shall be the id of an <value-of select="'$element-name'"/> element.</assert>
    </rule>
  </pattern>

  <pattern id="AbstractSiblingListMatchCheck" abstract="true">
    <!-- Verifies that the number of values in sibling elements are the same.  Parameters are list1element-name and list2element-name. -->
    <rule context="n42:$list1element-name">
      <assert test="(count(tokenize(normalize-space(string(.)),'\s+')) = count(tokenize(normalize-space(string(../n42:$list2element-name)),'\s+')))">Sibling <value-of select="'$list1element-name'"/> and <value-of select="'$list2element-name'"/> elements shall have the same number of values.</assert>
    </rule>
  </pattern>

  <!-- ************************ Instantiated abstract and directly defined patterns ************************ -->

  <pattern id="AnalysisDataReferencesExclusivityCheck">
    <rule context="n42:AnalysisResults">
      <assert test="not((@derivedDataReferences and @radMeasurementGroupReferences) or (derivedDataReferences and @radMeasurementReferences) or (@radMeasurementGroupReferences and @radMeasurementReferences)) and (@derivedDataReferences or @radMeasurementGroupReferences or @radMeasurementReferences)">One and only one of the AnalysisResults derivedDataReferences, radMeasurementGroupReferences, and radMeasurementReferences attributes shall be specified.</assert>
    </rule>
  </pattern>
  
  <pattern id="AnalysisStartDateCheck" is-a="AbstractFutureDateCheck">
    <param name="element-name" value="AnalysisStartDateTime"/>
  </pattern>

  <pattern id="CalibrationDateCheck" is-a="AbstractFutureDateCheck">
    <param name="element-name" value="CalibrationDateTime"/>
  </pattern>

  <pattern id="CharacteristicGroupOOLConsistencyCheck">
    <rule context="n42:CharacteristicGroup[@groupOutOfLimits=false()]">
      <assert test="count(n42:Characteristic[@valueOutOfLimits=true()]) = 0">The CharacteristicGroup groupOutOfLimits attribute shall be true if any of descendant Characteristic valueOutOfLimits attributes are true.</assert>
    </rule>
  </pattern>
  
  <pattern id="CompressionCheck">
    <rule context="n42:ChannelData[@compressionCode='CountedZeroes']">
      <let name="list-as-seq" value="tokenize(normalize-space(string(.)),'\s+')"/>
      <let name="list-length" value="count($list-as-seq)"/>
      <assert test="number($list-as-seq[$list-length]) != 0">The compressed content of ChannelData shall be coherent: the last value in the list shall not be zero.</assert>
      <!-- See previous comments on use of empty() and index-of() to process the output of a for expression -->
      <assert test="empty(index-of(for $x in 1 to $list-length - 1 return (number($list-as-seq[$x]) != 0) or (number($list-as-seq[$x]) = 0 and number($list-as-seq[$x + 1]) != 0 and (floor(number($list-as-seq[$x + 1]))) = number($list-as-seq[$x + 1])),false()))">The compressed content of ChannelData shall be coherent: all zero values in the list shall be followed by a value that is greater than zero and an integer.</assert>
    </rule>
  </pattern>

  <pattern id="CountDataValuesCheck">
    <rule context="n42:GrossCounts">
      <assert test="@energyWindowsReference or (not(@energyWindowsReference) and count(tokenize(normalize-space(string(n42:CountData)),'\s+')) = 1)"> If the GrossCounts element's energyWindowsReference attribute is omitted, the child CountData element shall have exactly one value.</assert>
      <assert test="not(@energyWindowsReference) or (count(tokenize(normalize-space(string(id(@energyWindowsReference)/n42:WindowStartEnergyValues)),'\s+')) = count(tokenize(normalize-space(string(n42:CountData)),'\s+')))"> If the GrossCounts element's energyWindowsReference attribute is present, the child CountData element and the referenced EnergyWindows shall have the same number of values.</assert>
    </rule>
  </pattern>

  <pattern id="DerivedDataReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="derivedDataReferences"/>
    <param name="element-name" value="DerivedData"/>
  </pattern>
  
  <pattern id="EBoundariesChannelsCheck">
    <!-- This is the most complicated rule.  The steps are:
      1. Convert the list in ChannelData to a sequence
      2. Get the number of items in that sequence
      3. Determine if the ChannelData data is compressed
      4. Get the number of values in the referenced EnergyBoundaryValues (if any).  HACK ALERT: this logic depends on access to a non-existent element not doing anything disastrous.
      5. In case the data was compressed, count the number of zero channels in the sequence. Then get the number of non-zero channels, which is the number of values minus 2 * the number of zero values.
      6. Finally get the real number of channels in the spectrum: if the data is not compressed, it's just the number of values in the sequence; otherwise it is the sum of the number of zero and non-zero channels calculated in the last step. 
      7. After all this, the actual test is easy: if EnergyBoundaryValues is used for the calibration, the number of items in that element must be one greater than the number of channels.
    -->
    <rule context="n42:Spectrum">
      <let name="cd-list-as-seq" value="tokenize(normalize-space(string(n42:ChannelData)),'\s+')"/>
      <let name="cd-list-length" value="count($cd-list-as-seq)"/>
      <let name="cd-is-compressed" value="boolean(n42:ChannelData/@compressionCode = 'CountedZeroes')"/>
      <let name="eb-channels" value="count(tokenize(normalize-space(string(id(@energyCalibrationReference)/n42:EnergyBoundaryValues)),'\s+'))"/>
      <let name="cd-decompressed-zeroes" value="sum(for $x in 1 to $cd-list-length - 1 return (number(number($cd-list-as-seq[$x]) = 0) * number($cd-list-as-seq[$x + 1])) + (number(number($cd-list-as-seq[$x]) != 0) * 0))"/>
      <let name="cd-decompressed-nonzeroes" value="$cd-list-length - 2 * sum(for $x in 1 to $cd-list-length - 1 return number(number($cd-list-as-seq[$x]) = 0))"/>
      <let name="cd-channels" value="(number($cd-is-compressed) * ($cd-decompressed-zeroes + $cd-decompressed-nonzeroes)) + (number(not($cd-is-compressed)) * $cd-list-length)"/>
      <assert test="($eb-channels = 0) or ($eb-channels = $cd-channels + 1)">If the energy calibration of Spectrum is defined using EnergyBoundaryValues, then the number of values in EnergyBoundaryValues (<value-of select="number($eb-channels)"/>) shall be one greater than the number of channels contained in the Spectrum (<value-of select="number($cd-channels)"/>).</assert>
    </rule>
  </pattern>

  <pattern id="EfficiencyUncertaintyMatch" is-a="AbstractSiblingListMatchCheck">
    <param name="list1element-name" value="EfficiencyUncertaintyValues"/>
    <param name="list2element-name" value="EfficiencyValues"/>
  </pattern>

  <pattern id="EnergyBoundaryIncreasingValuesCheck" is-a="AbstractOrderedElementCheck">
    <param name="element-name" value="EnergyBoundaryValues"/>
  </pattern>

  <pattern id="EnergyCalibrationReferenceTypeMatch" is-a="AbstractReferenceTypeMatch">
    <param name="attribute-name" value="energyCalibrationReference"/>
    <param name="element-name" value="EnergyCalibration"/>
  </pattern>

  <pattern id="EnergyFWHMMatch" is-a="AbstractSiblingListMatchCheck">
    <param name="list1element-name" value="FWHMValues"/>
    <param name="list2element-name" value="EnergyValues"/>
  </pattern>

  <pattern id="EnergyEfficiencyMatch" is-a="AbstractSiblingListMatchCheck">
    <param name="list1element-name" value="EfficiencyValues"/>
    <param name="list2element-name" value="EnergyValues"/>
  </pattern>

  <pattern id="EnergyIncreasingListValuesCheck" is-a="AbstractOrderedElementCheck">
    <param name="element-name" value="EnergyValues"/>
  </pattern>

  <pattern id="FullEnergyPECalibrationReferenceTypeMatch" is-a="AbstractReferenceTypeMatch">
    <param name="attribute-name" value="fullEnergyPeakEfficiencyCalibrationReference"/>
    <param name="element-name" value="FullEnergyPeakEfficiencyCalibration"/>
  </pattern>
  
  <pattern id="FWHMCalibrationReferenceTypeMatch" is-a="AbstractReferenceTypeMatch">
    <param name="attribute-name" value="FWHMCalibrationReference"/>
    <param name="element-name" value="FWHMCalibration"/>
  </pattern>

  <pattern id="IDoubleEscapePECalibrationReferenceTypeMatch" is-a="AbstractReferenceTypeMatch">
    <param name="attribute-name" value="intrinsicDoubleEscapePeakEfficiencyCalibrationReference"/>
    <param name="element-name" value="IntrinsicDoubleEscapePeakEfficiencyCalibration"/>
  </pattern>

  <pattern id="IFullEnergyPECalibrationReferenceTypeMatch" is-a="AbstractReferenceTypeMatch">
    <param name="attribute-name" value="intrinsicFullEnergyPeakEfficiencyCalibrationReference"/>
    <param name="element-name" value="IntrinsicFullEnergyPeakEfficiencyCalibration"/>
  </pattern>

  <pattern id="InspectionDateCheck" is-a="AbstractFutureDateCheck">
    <param name="element-name" value="InspectionDateTime"/>
  </pattern>

  <pattern id="ISingleEscapePECalibrationReferenceTypeMatch" is-a="AbstractReferenceTypeMatch">
    <param name="attribute-name" value="intrinsicSingleEscapePeakEfficiencyCalibrationReference"/>
    <param name="element-name" value="IntrinsicSingleEscapePeakEfficiencyCalibration"/>
  </pattern>

  <pattern id="N42DocDateCheck" is-a="AbstractAttributeFutureDateCheck">
    <param name="attribute-name" value="n42DocDateTime"/>
  </pattern>

  <pattern id="OriginReferenceCircularReferenceCheck">
    <!-- This test just checks for 'immediate' circularity (e.g., originReference points to the same object to which its ancestor RadxxxState radxxxInformationReference points).  Someday do better... -->
    <rule context="n42:RadInstrumentState/n42:StateVector/n42:RelativeLocation/n42:Origin/@originReference">
      <assert test="local-name(id(.)) != 'RadInstrumentInformation'">The use of the originReference attribute shall not result in a circular reference.</assert>
    </rule>
    <rule context="n42:RadDetectorState/n42:StateVector/n42:RelativeLocation/n42:Origin/@originReference">
      <assert test=". != ../../../../@radDetectorInformationReference">The use of the originReference attribute shall not result in a circular reference.</assert>
    </rule>
    <rule context="n42:RadItemState/n42:StateVector/n42:RelativeLocation/n42:Origin/@originReference">
      <assert test=". != ../../../../@radItemInformationReference">The use of the originReference attribute shall not result in a circular reference.</assert>
    </rule>
  </pattern>
  
  <pattern id="OriginReferenceDescriptionCheck" is-a="AbstractAttributeElementCheck">
    <param name="attribute-name" value="originReference"/>
    <param name="element-name" value="OriginDescription"/>
  </pattern>

  <pattern id="OriginReferenceTypeMatch">
    <rule context="@originReference">
      <assert
        test="local-name(id(.)) = 'RadInstrumentInformation' or
              local-name(id(.)) = 'RadDetectorInformation' or
              local-name(id(.)) = 'RadItemInformation'"> The value of a originReference attribute shall be the id of a RadInstrumentInformation, RadDetectorInformation, or RadItemInformation element.</assert>
    </rule>
  </pattern>

  <pattern id="RadAlarmCategoryCheck" is-a="AbstractOtherDescriptionCheck">
    <param name="parentelement-name" value="RadAlarm"/>
    <param name="childelement-name" value="RadAlarmCategoryCode"/>
    <param name="descriptionelement-name" value="RadAlarmDescription"/>
  </pattern>

  <pattern id="RadAlarmDateCheck" is-a="AbstractFutureDateCheck">
    <param name="element-name" value="RadAlarmDateTime"/>
  </pattern>

  <pattern id="RadDetectorCategoryCheck" is-a="AbstractOtherDescriptionCheck">
    <param name="parentelement-name" value="RadDetectorInformation"/>
    <param name="childelement-name" value="RadDetectorCategoryCode"/>
    <param name="descriptionelement-name" value="RadDetectorDescription"/>
  </pattern>

  <pattern id="RadDetectorInformationReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radDetectorInformationReferences"/>
    <param name="element-name" value="RadDetectorInformation"/>
  </pattern>

  <pattern id="RadDetectorInformationReferenceTypeMatch" is-a="AbstractReferenceTypeMatch">
    <param name="attribute-name" value="radDetectorInformationReference"/>
    <param name="element-name" value="RadDetectorInformation"/>
  </pattern>

  <pattern id="RadDetectorKindCheck" is-a="AbstractOtherDescriptionCheck">
    <param name="parentelement-name" value="RadDetectorInformation"/>
    <param name="childelement-name" value="RadDetectorKindCode"/>
    <param name="descriptionelement-name" value="RadDetectorDescription"/>
  </pattern>

  <pattern id="RadItemInformationReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radItemInformationReferences"/>
    <param name="element-name" value="RadItemInformation"/>
  </pattern>

  <pattern id="RadMeasurementGroupReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radMeasurementGroupReferences"/>
    <param name="element-name" value="RadMeasurementGroup"/>
  </pattern>

  <pattern id="RadMeasurementReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radMeasurementReferences"/>
    <param name="element-name" value="RadMeasurement"/>
  </pattern>

  <pattern id="RadMeasurementUniqueDetectorCheck-DoseRate"
    is-a="AbstractElementReferenceCardinalityCheck">
    <param name="parentelement-name" value="RadMeasurement"/>
    <param name="childelement-name" value="DoseRate"/>
    <param name="attribute-name" value="radDetectorInformationReference"/>
    <param name="reference-type" value="detector"/>
  </pattern>

  <pattern id="RadMeasurementUniqueDetectorCheck-ExposureRate"
    is-a="AbstractElementReferenceCardinalityCheck">
    <param name="parentelement-name" value="RadMeasurement"/>
    <param name="childelement-name" value="ExposureRate"/>
    <param name="attribute-name" value="radDetectorInformationReference"/>
    <param name="reference-type" value="detector"/>
  </pattern>

  <pattern id="RadMeasurementUniqueDetectorCheck-GrossCounts"
    is-a="AbstractElementReferenceCardinalityCheck">
    <param name="parentelement-name" value="RadMeasurement"/>
    <param name="childelement-name" value="GrossCounts"/>
    <param name="attribute-name" value="radDetectorInformationReference"/>
    <param name="reference-type" value="detector"/>
  </pattern>

  <pattern id="RadMeasurementUniqueDetectorCheck-Spectrum"
    is-a="AbstractElementReferenceCardinalityCheck">
    <param name="parentelement-name" value="RadMeasurement"/>
    <param name="childelement-name" value="Spectrum"/>
    <param name="attribute-name" value="radDetectorInformationReference"/>
    <param name="reference-type" value="detector"/>
  </pattern>

  <pattern id="RadMeasurementUniqueDetectorCheck-TotalDose"
    is-a="AbstractElementReferenceCardinalityCheck">
    <param name="parentelement-name" value="RadMeasurement"/>
    <param name="childelement-name" value="TotalDose"/>
    <param name="attribute-name" value="radDetectorInformationReference"/>
    <param name="reference-type" value="detector"/>
  </pattern>

  <pattern id="RadMeasurementUniqueDetectorCheck-TotalExposure"
    is-a="AbstractElementReferenceCardinalityCheck">
    <param name="parentelement-name" value="RadMeasurement"/>
    <param name="childelement-name" value="TotalExposure"/>
    <param name="attribute-name" value="radDetectorInformationReference"/>
    <param name="reference-type" value="detector"/>
  </pattern>

  <pattern id="RadRawDoseRateReferencesCheck" is-a="AbstractAttributeParentCheck">
    <param name="attribute-name" value="radRawDoseRateReferences"/>
    <param name="element-name" value="DerivedData"/>
  </pattern>

  <pattern id="RadRawDoseRateReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radRawDoseRateReferences"/>
    <param name="element-name" value="DoseRate"/>
  </pattern>

  <pattern id="RadRawExposureRateReferencesCheck" is-a="AbstractAttributeParentCheck">
    <param name="attribute-name" value="radRawExposureRateReferences"/>
    <param name="element-name" value="DerivedData"/>
  </pattern>

  <pattern id="RadRawExposureRateReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radRawExposureRateReferences"/>
    <param name="element-name" value="ExposureRate"/>
  </pattern>

  <pattern id="RadRawGrossCountsReferencesCheck" is-a="AbstractAttributeParentCheck">
    <param name="attribute-name" value="radRawGrossCountsReferences"/>
    <param name="element-name" value="DerivedData"/>
  </pattern>

  <pattern id="RadRawGrossCountsReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radRawGrossCountsReferences"/>
    <param name="element-name" value="GrossCounts"/>
  </pattern>

  <pattern id="RadRawSpectrumReferencesCheck" is-a="AbstractAttributeParentCheck">
    <param name="attribute-name" value="radRawSpectrumReferences"/>
    <param name="element-name" value="DerivedData"/>
  </pattern>

  <pattern id="RadRawSpectrumReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radRawSpectrumReferences"/>
    <param name="element-name" value="Spectrum"/>
  </pattern>

  <pattern id="RadRawTotalDoseReferencesCheck" is-a="AbstractAttributeParentCheck">
    <param name="attribute-name" value="radRawTotalDoseReferences"/>
    <param name="element-name" value="DerivedData"/>
  </pattern>

  <pattern id="RadRawTotalDoseReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radRawTotalDoseReferences"/>
    <param name="element-name" value="TotalDose"/>
  </pattern>

  <pattern id="RadRawTotalExposureReferencesCheck" is-a="AbstractAttributeParentCheck">
    <param name="attribute-name" value="radRawTotalExposureReferences"/>
    <param name="element-name" value="DerivedData"/>
  </pattern>

  <pattern id="RadRawTotalExposureReferencesTypeMatch" is-a="AbstractReferencesTypeMatch">
    <param name="attribute-name" value="radRawTotalExposureReferences"/>
    <param name="element-name" value="TotalExposure"/>
  </pattern>

  <pattern id="StartDateCheck" is-a="AbstractFutureDateCheck">
    <param name="element-name" value="StartDateTime"/>
  </pattern>

  <pattern id="TotalEfficiencyCalibrationReferenceTypeMatch" is-a="AbstractReferenceTypeMatch">
    <param name="attribute-name" value="totalEfficiencyEfficiencyCalibrationReference"/>
    <param name="element-name" value="TotalEfficiencyEfficiencyCalibration"/>
  </pattern>
  
  <pattern id="ValueDateCheck" is-a="AbstractAttributeFutureDateCheck">
    <param name="attribute-name" value="valueDateTime"/>
  </pattern>
  
  <pattern id="WindowStartEndCheck" is-a="AbstractPairedElementOrderCheck">
    <param name="parent-name" value="EnergyWindows"/>
    <param name="element1-name" value="WindowStartEnergyValues"/>
    <param name="element2-name" value="WindowEndEnergyValues"/>
  </pattern>

  <pattern id="WindowStartEndMatch" is-a="AbstractSiblingListMatchCheck">
    <param name="list1element-name" value="WindowStartEnergyValues"/>
    <param name="list2element-name" value="WindowEndEnergyValues"/>
  </pattern>

  <pattern id="WindowStartIncreasingListValuesCheck" is-a="AbstractNonDecreasingElementCheck">
    <param name="element-name" value="WindowStartEnergyValues"/>
  </pattern>

</schema>
