/*******************************************************************************
 * Copyright (c) 2017-2019 Checkmarx
 *
 * This software is licensed for customer's internal use only.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package com.checkmarx.engine.rest.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.joda.time.DateTime;
import org.joda.time.Instant;

import com.checkmarx.engine.utils.ScanUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScanRequest {

	public enum ScanStatus {

		Unknown(0),
		New(1),
		PreScan(2),
		Queued(3),
		Scanning(4),
		Deleted(5), //FIXME: don't know what id is scan status deleted
		Finished(7),
		Canceled(8),
		Failed(9),
		SourcePullingAndDeployment(10);

		private int stageId;
		public int getStageId() {
			return stageId;
		}

		private ScanStatus(int stageId) {
			this.stageId = stageId;
		}

		public static ScanStatus from(long stageId) {
			for (ScanStatus state : ScanStatus.values()) {
				if (stageId == state.stageId)
					return state;
			}
			return ScanStatus.Unknown;
		}

		public static ScanStatus from(Stage stage) {
			return stage == null ? ScanStatus.Unknown : from(stage.getId());
		}

		public static Stage to(ScanStatus status) {
			return new Stage(status.stageId, status.name());
		}
	}

	//private static final String TZ = "America/Chicago";
	//private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	
	private final String POSTPONE = "Postpone";

	private long id;
	private String runId;
	private Stage stage;
	private String stageDetails;
	private String stepDetails;
	private String teamId;
	private Project project;
	private Engine engine;
	private Integer loc;
	private ProgramLanguage[] languages;
	//@JsonFormat(shape=JsonFormat.Shape.STRING, pattern=DATE_PATTERN, timezone=TZ)
	private DateTime dateCreated;
	//@JsonFormat(shape=JsonFormat.Shape.STRING, pattern=DATE_PATTERN, timezone=TZ)
	private DateTime queuedOn;
	//@JsonFormat(shape=JsonFormat.Shape.STRING, pattern=DATE_PATTERN, timezone=TZ)
	private DateTime engineStartedOn;
	private DateTime completedOn;
	@JsonProperty(value = "isIncremental")
	private boolean incremental;
	@JsonProperty(value = "isPublic")
	private boolean isPublic;
	private String origin;
	private Integer queuePosition;
	private Integer totalPercent;
	private Integer stagePercent;
	private String initiator;

	public ScanRequest() {
		// default .ctor
	}

	public ScanRequest(long id, String runId, String teamId, Project project, Stage stage,
					   String stageDetails, String stepDetails, Integer totalPercent, Integer stagePercent, 
					   Integer loc, boolean incremental, boolean isPublic, String origin,
					   Integer queuePosition, String initiator, ProgramLanguage[] languages, 
					   DateTime dateCreated, DateTime queuedOn, DateTime engineStartedOn, DateTime completedOn
	) {
		this.id = id;
		this.runId = runId;
		this.stage = stage;
		this.stageDetails = stageDetails;
		this.stepDetails = stepDetails;
		this.totalPercent = totalPercent;
		this.stagePercent = stagePercent;
		this.teamId = teamId;
		this.project = project;
		this.loc = loc;
		this.languages = languages;
		this.dateCreated = dateCreated;
		this.queuedOn = queuedOn;
		this.engineStartedOn = engineStartedOn;
		this.completedOn = completedOn;
		this.incremental = incremental;
		this.isPublic = isPublic;
		this.origin = origin;
		this.queuePosition = queuePosition;
		this.initiator = initiator;
	}

	/**
	 * ScanRequest.Id field
	 */
	public long getId() {
		return id;
	}

	public String getRunId() {
		return runId;
	}

	@JsonIgnore
	public ScanStatus getStatus() {
		return ScanStatus.from(stage);
	}

	public Stage getStage() {
		return stage;
	}

	public String getStageDetails() {
		return stageDetails;
	}

	public String getStepDetails() {
		return stepDetails;
	}

	public String getTeamId() {
		return teamId;
	}

	public Project getProject() {
		return project;
	}

	public Long getEngineId() {
		return engine == null ? null : engine.getId();
	}

	public Engine getEngine() {
		return engine;
	}

	public Integer getLoc() {
		if(loc == null)
			return 0;
		return loc;
	}

	public List<ProgramLanguage> getLanguages() {
		return Arrays.asList(languages);
	}

	public DateTime getDateCreated() {
		return dateCreated;
	}

	public DateTime getQueuedOn() {
		return queuedOn;
	}

	public DateTime getEngineStartedOn() {
		return engineStartedOn;
	}

	public DateTime getCompletedOn() {
		return completedOn;
	}

	public boolean isIncremental() {
		return incremental;
	}
	
	public boolean isPostponed() {
		return POSTPONE.equalsIgnoreCase(stageDetails);
	}

	public boolean isPublic() {
		return isPublic;
	}

	public String getOrigin() {
		return origin;
	}

	public Integer getQueuePosition() {
		return queuePosition;
	}

	public Integer getTotalPercent() {
		return totalPercent;
	}

	public Integer getStagePercent() {
		return stagePercent;
	}

	public String getInitiator() {
		return initiator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final ScanRequest other = (ScanRequest) obj;
		return Objects.equals(this.id, other.id);
	}

	private String printLanguages() {
		final StringBuilder sb = new StringBuilder();
		getLanguages().forEach(lang -> sb.append(lang.toString() + ", "));
		return sb.toString();
	}

	protected MoreObjects.ToStringHelper toStringHelper() {
		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.add("stage", getStatus())
				.add("stageDetails", stageDetails)
				.add("projectId", project.getId())
				.add("engineId", getEngineId())
				.add("loc", loc)
				.add("dateCreated", ScanUtils.printDate(dateCreated))
				.add("queuedOn", ScanUtils.printDate(queuedOn))
				.add("queuePosition", queuePosition);
	}

	@Override
	public String toString() {
		return toStringHelper()
				.toString();
	}

	public String toString(boolean includeAll) {
		if (!includeAll) return toString();

		return toStringHelper()
				.add("project", project)
				//.add("stage", stage)
				.add("stepDetails", stepDetails)
				.add("totalPercent", totalPercent)
				.add("stagePercent", stagePercent)
				.add("teamId", teamId)
				.add("languages", printLanguages())
				.add("queuedOn", queuedOn)
				.add("engineStartedOn", printTime(engineStartedOn))
				.add("completedOn", printTime(completedOn))
				.add("incremental", incremental)
				.add("isPublic", isPublic)
				.add("origin", origin)
				.add("initiator", initiator)
				.omitNullValues()
				.toString();
	}
	
	private Instant printTime(DateTime time) {
	    return time == null ? null : time.toInstant();
	}
}