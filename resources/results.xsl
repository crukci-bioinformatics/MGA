<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" indent="yes"/>
<xsl:variable name="assignedFractionThreshold">0.01</xsl:variable>
<xsl:variable name="alignedFractionThreshold">0.01</xsl:variable>
<xsl:variable name="errorRateThreshold">0.0125</xsl:variable>
<xsl:template match="/">
<html>
<head>
<title>Multi-Genome Alignment Report
	<xsl:choose>
		<xsl:when test="MultiGenomeAlignmentSummaries/Properties/Property[@name='Run name']">
			&#8211; <xsl:value-of select="MultiGenomeAlignmentSummaries/Properties/Property[@name='Run name']/@value"/>
		</xsl:when>
		<xsl:when test="MultiGenomeAlignmentSummaries/RunId">
			&#8211; Run <xsl:value-of select="MultiGenomeAlignmentSummaries/RunId"/>
		</xsl:when>
	</xsl:choose>
</title>
</head>
<body>

<h2>Multi-Genome Alignment Report</h2>

<xsl:variable name="datasetCount"><xsl:value-of select="count(MultiGenomeAlignmentSummaries/MultiGenomeAlignmentSummary)"/></xsl:variable>

<!-- Variables taken from results for first dataset -->

<xsl:variable name="referenceGenomeCount"><xsl:value-of select="count(MultiGenomeAlignmentSummaries/ReferenceGenomes/ReferenceGenome)"/></xsl:variable>

<xsl:variable name="trimStart"><xsl:value-of select="MultiGenomeAlignmentSummaries/TrimStart"/></xsl:variable>
<xsl:variable name="trimLength"><xsl:value-of select="MultiGenomeAlignmentSummaries/TrimLength"/></xsl:variable>

<xsl:variable name="totalSequenceCount"><xsl:value-of select="sum(MultiGenomeAlignmentSummaries/MultiGenomeAlignmentSummary/SequenceCount)"/></xsl:variable>

<xsl:variable name="yieldMultiplier">
	<xsl:choose>
		<xsl:when test="MultiGenomeAlignmentSummaries/Properties/Property[@name='End type']/@value = 'Paired End'">2</xsl:when>
		<xsl:otherwise>1</xsl:otherwise>
	</xsl:choose>
</xsl:variable>

<table>
	<xsl:for-each select="MultiGenomeAlignmentSummaries/Properties/Property[@name]">
		<tr>
			<td><xsl:value-of select="@name"/>:</td>
			<td><xsl:value-of select="@value"/></td>
		</tr>
	</xsl:for-each>
	<xsl:variable name="totalYield"><xsl:value-of select="$yieldMultiplier * $totalSequenceCount * MultiGenomeAlignmentSummaries/Properties/Property[@name='Cycles']/@value div 1000000000"/></xsl:variable>
	<xsl:if test="$totalYield != 'NaN'">
		<tr>
			<td>Yield (Gbases):</td>
			<td><xsl:value-of select="format-number($totalYield, '0.00')"/></td>
		</tr>
	</xsl:if>
	<tr>
		<td>Total sequences:</td>
		<td><xsl:value-of select="format-number($totalSequenceCount, '###,###')"/></td>
	</tr>
</table>

<p/>

<xsl:param name="image"/>

<xsl:if test="$image">
	<img>
		<xsl:attribute name="src">data:image/png;base64,<xsl:value-of select="$image"/></xsl:attribute>
	</img>
</xsl:if>

<br/>
Sequences were sampled<xsl:if test="$trimLength != ''">,
trimmed to <xsl:value-of select="$trimLength"/> bases</xsl:if><xsl:if test="$trimStart != ''">
starting from position <xsl:value-of select="$trimStart"/>,</xsl:if>
and mapped to <xsl:value-of select="$referenceGenomeCount"/>
reference genomes (see <a href="#referenceGenomes">list</a> below) using Bowtie.
Sequences containing adapters were found by ungapped alignment of the full length
sequence to a set of known adapter and primer sequences using Exonerate.
Further details on the alignment results and the assignment of reads to
genomes are given <a href="#alignmentDetails">below</a>.
<p/>

Datasets
<xsl:for-each select="MultiGenomeAlignmentSummaries/MultiGenomeAlignmentSummary">
	<xsl:sort select="DatasetId"/>
	<xsl:variable name="datasetId"><xsl:value-of select="DatasetId"/></xsl:variable>
	| <a href="#{$datasetId}"><xsl:value-of select="DatasetId"/></a>
</xsl:for-each>
<p/>

<xsl:for-each select="MultiGenomeAlignmentSummaries/MultiGenomeAlignmentSummary">
	<xsl:sort select="DatasetId"/>

	<hr/>
	<xsl:variable name="datasetId"><xsl:value-of select="DatasetId"/></xsl:variable>
	<h3 id="{$datasetId}"><xsl:value-of select="DatasetId"/></h3>

	<table>
		<xsl:variable name="yield"><xsl:value-of select="$yieldMultiplier * SequenceCount * ../Properties/Property[@name='Cycles']/@value div 1000000000"/></xsl:variable>
		<xsl:if test="$yield != 'NaN'">
			<tr>
				<td>Yield (Gbases):</td>
				<td><xsl:value-of select="format-number($yield, '0.00')"/></td>
			</tr>
		</xsl:if>
		<tr>
			<td>Sequences:</td>
			<xsl:choose>
				<xsl:when test="SequenceCount &gt; 0">
					<td align="right"><xsl:value-of select="format-number(SequenceCount, '###,###')"/></td>
				</xsl:when>
				<xsl:otherwise>
					<td align="left"><xsl:value-of select="SequenceCount"/></td>
				</xsl:otherwise>
			</xsl:choose>
		</tr>
		<xsl:if test="SampledCount &gt; 0">
			<tr>
				<td>Sampled:</td>
				<td><xsl:value-of select="format-number(SampledCount, '###,###')"/></td>
			</tr>
		</xsl:if>
	</table>
	<p/>

	<xsl:if test="SampledCount &gt; 0">

		<xsl:variable name="otherCount"><xsl:value-of select="sum(AlignmentSummaries/AlignmentSummary[not(ReferenceGenome/@name = ../../Samples/Sample/Properties/Property[@name='Species']/@value) and AssignedCount div ../../SampledCount &lt; $assignedFractionThreshold and (AlignedCount div ../../SampledCount &lt; $alignedFractionThreshold or ErrorRate &gt; $errorRateThreshold)]/AssignedCount)"/></xsl:variable>
		<xsl:variable name="otherNumber"><xsl:value-of select="count(AlignmentSummaries/AlignmentSummary[not(ReferenceGenome/@name = ../../Samples/Sample/Properties/Property[@name='Species']/@value) and AssignedCount div ../../SampledCount &lt; $assignedFractionThreshold and (AlignedCount div ../../SampledCount &lt; $alignedFractionThreshold or ErrorRate &gt; $errorRateThreshold)]/AssignedCount)"/></xsl:variable>

		<table border="2" cellpadding="5" style="border-collapse: collapse">

			<tr align="left">
				<th>Reference ID</th>
				<th>Species/Reference Genome</th>
				<th>Aligned</th>
				<th>Aligned %</th>
				<th>Error rate</th>
				<th>Unique</th>
				<th>Error rate</th>
				<th>Best</th>
				<th>Error rate</th>
				<th>Assigned</th>
				<th>Assigned %</th>
				<th>Error rate</th>
			</tr>

			<xsl:for-each select="AlignmentSummaries/AlignmentSummary">
				<xsl:sort select="AssignedCount" data-type="number" order="descending"/>

				<xsl:variable name="alignedFraction"><xsl:value-of select="AlignedCount div ../../SampledCount"/></xsl:variable>
				<xsl:variable name="assignedFraction"><xsl:value-of select="AssignedCount div ../../SampledCount"/></xsl:variable>

				<xsl:if test="$otherNumber &lt; 2 or not($assignedFraction &lt; $assignedFractionThreshold) or (not($alignedFraction &lt; $alignedFractionThreshold) and not(ErrorRate &gt; $errorRateThreshold)) or ReferenceGenome/@name = ../../Samples/Sample/Properties/Property[@name='Species']/@value">

				<tr>
					<xsl:choose>
						<xsl:when test="ReferenceGenome/@name = ../../Samples/Sample/Properties[Property[@name='Control' and @value='Yes' ]]/Property[@name='Species']/@value">
							<xsl:attribute name="style">
								background-color:
								<xsl:choose>
									<xsl:when test="$assignedFraction &gt; 0.15 and AssignedErrorRate &lt; 0.4">#FFA000</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.10 and AssignedErrorRate &lt; 0.5">#FFB400</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.05 and AssignedErrorRate &lt; 0.75">#FFC800</xsl:when>
									<xsl:otherwise>#FFDC00</xsl:otherwise>
								</xsl:choose>
								;
							</xsl:attribute>
						</xsl:when>
						<xsl:when test="ReferenceGenome/@name = ../../Samples/Sample/Properties/Property[@name='Species']/@value">
							<xsl:attribute name="style">
								background-color:
								<xsl:choose>
									<xsl:when test="$assignedFraction &gt; 0.8 and AssignedErrorRate &lt; 0.5">#58FA58</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.6 and AssignedErrorRate &lt; 0.6">#81F781</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.4 and AssignedErrorRate &lt; 0.7">#A9F5A9</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.2 and AssignedErrorRate &lt; 0.8">#CEF6CE</xsl:when>
									<xsl:otherwise>#E0F8E0</xsl:otherwise>
								</xsl:choose>
								;
							</xsl:attribute>
						</xsl:when>
						<xsl:when test="../../Samples/Sample/Properties/Property[@name='Species']/@value = 'Other'"/>
						<xsl:when test="../../Samples/Sample/Properties/Property[@name='Species']/@value = 'other'"/>
						<xsl:when test="../../Samples/Sample/Properties/Property[@name='Species']/@value != ''">
							<xsl:attribute name="style">
								background-color:
								<xsl:choose>
									<xsl:when test="$assignedFraction &gt; 0.4 and AssignedErrorRate &lt; 0.5">#FE2E2E</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.3 and AssignedErrorRate &lt; 0.6">#FA5858</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.2 and AssignedErrorRate &lt; 0.7">#F78181</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.1 and AssignedErrorRate &lt; 0.8">#F5A9A9</xsl:when>
									<xsl:when test="$assignedFraction &gt; 0.05 and AssignedErrorRate &lt; 1.0">#F6CECE</xsl:when>
								</xsl:choose>
								;
							</xsl:attribute>
						</xsl:when>
					</xsl:choose>

					<td><xsl:value-of select="ReferenceGenome/@id"/></td>
					<td><xsl:value-of select="ReferenceGenome/@name"/></td>
					<td align="right">
						<xsl:value-of select="AlignedCount"/>
					</td>
					<td align="right">
						<xsl:if test="AlignedCount &gt; 0">
							<xsl:value-of select="format-number($alignedFraction, '0.0%')"/>
						</xsl:if>
					</td>
					<td align="right">
						<xsl:if test="AlignedCount &gt; 0 and ErrorRate != ''">
							<xsl:value-of select="format-number(ErrorRate, '0.00%')"/>
						</xsl:if>
					</td>
					<td align="right">
						<xsl:value-of select="UniquelyAlignedCount"/>
					</td>
					<td align="right">
						<xsl:if test="UniquelyAlignedCount &gt; 0 and UniquelyAlignedErrorRate != ''">
							<xsl:value-of select="format-number(UniquelyAlignedErrorRate, '0.00%')"/>
						</xsl:if>
					</td>
					<td align="right">
						<xsl:value-of select="PreferentiallyAlignedCount"/>
					</td>
					<td align="right">
						<xsl:if test="PreferentiallyAlignedCount &gt; 0 and PreferentiallyAlignedErrorRate != ''">
							<xsl:value-of select="format-number(PreferentiallyAlignedErrorRate, '0.00%')"/>
						</xsl:if>
					</td>
					<td align="right">
						<xsl:if test="AssignedCount &gt; 0">
							<xsl:value-of select="AssignedCount"/>
						</xsl:if>
					</td>
					<td align="right">
						<xsl:if test="AssignedCount &gt; 0">
							<xsl:value-of select="format-number($assignedFraction, '0.0%')"/>
						</xsl:if>
					</td>
					<td align="right">
						<xsl:if test="AssignedCount &gt; 0 and AssignedErrorRate != ''">
							<xsl:value-of select="format-number(AssignedErrorRate, '0.00%')"/>
						</xsl:if>
					</td>
				</tr>

				</xsl:if>
			</xsl:for-each>

			<xsl:if test="$otherNumber &gt; 1">
				<tr>
					<td>Other</td>
					<td><xsl:value-of select="$otherNumber"/> others</td>
					<td align="right">
						<xsl:value-of select="$otherCount"/>
					</td>
					<td align="right">
						<xsl:value-of select="format-number($otherCount div SampledCount, '0.0%')"/>
					</td>
				</tr>
			</xsl:if>

			<tr>
				<xsl:variable name="unmappedFraction"><xsl:value-of select="UnmappedCount div SampledCount"/></xsl:variable>
				<xsl:choose>
					<xsl:when test="Samples/Sample/Properties/Property[@name='Experiment type']/@value = 'sRNA'"/>
					<xsl:when test="Samples/Sample/Properties/Property[@name='Species']/@value = 'Other'"/>
					<xsl:when test="Samples/Sample/Properties/Property[@name='Species']/@value = 'other'"/>
					<xsl:when test="Samples/Sample/Properties/Property[@name='Species']/@value != ''">
						<xsl:attribute name="style">
							background-color:
							<xsl:choose>
								<xsl:when test="$unmappedFraction &gt; 0.4">#FE2E2E</xsl:when>
								<xsl:when test="$unmappedFraction &gt; 0.3">#FA5858</xsl:when>
								<xsl:when test="$unmappedFraction &gt; 0.2">#F78181</xsl:when>
								<xsl:when test="$unmappedFraction &gt; 0.1">#F5A9A9</xsl:when>
							</xsl:choose>
							;
						</xsl:attribute>
					</xsl:when>
				</xsl:choose>
				<td>Unmapped</td>
				<td></td>
				<td align="right"><xsl:value-of select="UnmappedCount"/></td>
				<td align="right"><xsl:value-of select="format-number($unmappedFraction, '0.0%')"/></td>
			</tr>

			<tr>
				<xsl:variable name="adapterFraction"><xsl:value-of select="AdapterCount div SampledCount"/></xsl:variable>
				<xsl:choose>
					<xsl:when test="Samples/Sample/Properties/Property[@name='Experiment type']/@value = 'sRNA'"/>
					<xsl:otherwise>
						<xsl:attribute name="style">
							background-color:
							<xsl:choose>
								<xsl:when test="$adapterFraction &gt; 0.4">#FE2E2E</xsl:when>
								<xsl:when test="$adapterFraction &gt; 0.3">#FA5858</xsl:when>
								<xsl:when test="$adapterFraction &gt; 0.2">#F78181</xsl:when>
								<xsl:when test="$adapterFraction &gt; 0.1">#F5A9A9</xsl:when>
							</xsl:choose>
							;
						</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
				<td>Adapter</td>
				<td></td>
				<td align="right"><xsl:value-of select="AdapterCount"/></td>
				<td align="right"><xsl:value-of select="format-number($adapterFraction, '0.0%')"/></td>
			</tr>

		</table>

	</xsl:if>

	<!--
		Assumes that sample properties are consistent for each sample in the dataset,
		i.e. have the same names and in the same order.
	-->
	<xsl:if test="Samples/Sample/Properties/Property[@name]">
		<br/>
		<table>
			<tr>
				<td>Sample details</td>
			</tr>
		</table>
		<br/>
		<table border="1" cellpadding="5" style="border-collapse: collapse">
			<tr align="left">
				<xsl:for-each select="Samples/Sample[1]/Properties/Property[@name]">
					<th><xsl:value-of select="@name"/></th>
				</xsl:for-each>
			</tr>
			<xsl:for-each select="Samples/Sample">
				<tr>
					<xsl:for-each select="Properties/Property[@name]">
						<td><xsl:value-of select="@value"/></td>
					</xsl:for-each>
				</tr>
			</xsl:for-each>
		</table>
	</xsl:if>

</xsl:for-each>

<p/>

<hr/>

<h3 id="alignmentDetails">Alignment Details</h3>

Reference genomes are sorted according to how many sequence reads have been
assigned to each. Separate entries are given for reference genomes for which at
least 1% of reads have been assigned or for which at least 1% of reads align
with an average mismatch or error rate of below 1.25%.
<p/>
In addition to the total number of reads aligning to each reference genome and
the average error rate for those alignments, details are also provided for the
the number of reads aligning uniquely to the reference genome and and the
associated error rate for those unique reads.
<p/>
The 'Best' column and accompanying error rate refer to those reads that align
preferentially to the given reference genome, i.e. with the fewest mismatches.
These reads will include those that align uniquely and those that also align
to other genomes with the same number of mismatches but which do not align to
another genome with fewer mismatches.
<p/>
Reads that align uniquely to a genome are assigned to that genome. Reads that
align equally well to multiple genomes are assigned to the genome with the
highest number of reads in the 'Best' column.
<p/>
Note that because reads are trimmed prior to alignment with Bowtie, it is
possible for a read to be counted both as aligned to one or more of the
reference genomes and among the reads with adapter content. The adapter will
most likely be present in the portion of the read that has been trimmed.
<p/>

<hr/>

<h3 id="referenceGenomes">Reference Genomes</h3>

Sequences were aligned to the following reference genomes
(<xsl:value-of select="$referenceGenomeCount"/> in total)

<ul style="list-style-type: circle">
	<xsl:for-each select="MultiGenomeAlignmentSummaries/ReferenceGenomes/ReferenceGenome">
		<xsl:sort select="@name"/>
		<li>
			<xsl:value-of select="@name"/>
		</li>
	</xsl:for-each>
</ul>

</body>
</html>

</xsl:template>
</xsl:stylesheet>

