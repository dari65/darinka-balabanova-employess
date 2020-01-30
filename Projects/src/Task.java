
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public final class Task {

	public static void main(String[] args) throws IOException {
		Map<String, List<ProjectData>> result = new HashMap<>();
		Map<CommonTimeProjectData, Long> timeResults = new HashMap<>();
		List<CommonTimeProjectData> timeResultsInPairs = new ArrayList<CommonTimeProjectData>();
        String currentPath = System.getProperty("user.dir")+"\\src\\data\\";
		List<ProjectData> projects = downloadHotelFile(currentPath+"tmp.txt");

		result = projects.stream().collect(Collectors.groupingBy(ProjectData::getProject));
		
		for (Map.Entry<String, List<ProjectData>> entry:result.entrySet()){
			List<ProjectData> colleagues = entry.getValue();
			for (int i = 0; i < colleagues.size(); i++) {
				ProjectData member1 = colleagues.get(i);
				for (int j = i+1; j < colleagues.size(); j++) {
					ProjectData member2 = colleagues.get(j);
					if( !member1.getCode().equalsIgnoreCase(member2.getCode()) ) {
						long timeInProject = getTimeInProject(member1,member2);
						if( timeInProject != 0 ){
							timeResultsInPairs.add(new CommonTimeProjectData(member1.getCode(), member2.getCode(), entry.getKey(), timeInProject));
						}
					}
				}
			}
		}
		
		timeResultsInPairs.stream().forEach(p ->{
			CommonTimeProjectData pairMembersInMap = timeResults.keySet().stream()
					.filter(e->(e.getCodeMember1().equalsIgnoreCase(p.getCodeMember1()) && e.getCodeMember2().equalsIgnoreCase(p.getCodeMember2())
							|| (e.getCodeMember1().equalsIgnoreCase(p.getCodeMember2()) && e.getCodeMember2().equalsIgnoreCase(p.getCodeMember1()))))
					.findAny().orElse(null);
			if (pairMembersInMap != null){
				timeResults.put(pairMembersInMap, timeResults.get(pairMembersInMap) + p.getTimeInProject());
			} else {
				timeResults.put(p, p.getTimeInProject());
			}
		});
		
	    Entry<CommonTimeProjectData, Long> maxEntry = Collections.max(timeResults.entrySet(), 
	    		(Entry<CommonTimeProjectData, Long> e1, Entry<CommonTimeProjectData, Long> e2) -> e1.getValue()
	            .compareTo(e2.getValue()));	
	    
		if ( maxEntry != null ) {
			String member1 = maxEntry.getKey().getCodeMember1();
			String member2 = maxEntry.getKey().getCodeMember2();
			System.out.println("Employee ID #1  Employee ID #2  Project ID  Days worked");
			System.out.println("-------------------------------------------------------");
			timeResultsInPairs.stream().forEach(pair ->{
				if (pair.getCodeMember1().equalsIgnoreCase(member1) && pair.getCodeMember2().equalsIgnoreCase(member2)
							|| (pair.getCodeMember1().equalsIgnoreCase(member2) && pair.getCodeMember2().equalsIgnoreCase(member1))){
				System.out.println(String.format("      %s           %s            %s            %s", member1, member2, pair.getProject(), pair.getTimeInProject()));
				}
			});

		}
	}
	
	public static long getTimeInProject(ProjectData member1, ProjectData member2) {
		long timeInProject = 0;
		try {
			LocalDate fromDate1 = getNonNullDate(member1.getFromDate());
			LocalDate fromDate2 = getNonNullDate(member2.getFromDate());
			LocalDate toDate1 = getNonNullDate(member1.getToDate());
			LocalDate toDate2 = getNonNullDate(member2.getToDate());
			boolean member1StartedFirst = isDateBetween(fromDate1, toDate1, fromDate2);
			boolean member2StartedFirst = isDateBetween(fromDate2, toDate2, fromDate1);
			boolean member1FinishedFirst = !(toDate2.isBefore(toDate1));
			boolean member2FinishedFirst = !(toDate1.isBefore(toDate2));
			
			if(member1StartedFirst && member1FinishedFirst){
				timeInProject = ChronoUnit.DAYS.between(fromDate2,toDate1);
			}
			if(member1StartedFirst && member2FinishedFirst){
				timeInProject = ChronoUnit.DAYS.between(fromDate2,toDate2);			
			}
			if(member2StartedFirst && member1FinishedFirst){
				timeInProject = ChronoUnit.DAYS.between(fromDate1,toDate1);			
			}
			if(member2StartedFirst && member2FinishedFirst){
				timeInProject = ChronoUnit.DAYS.between(fromDate1,toDate2);			
			}
		} catch (DateTimeParseException e){
			throw e;
		}
		return timeInProject;
	}

	private static LocalDate getNonNullDate(String memberDate) {
		LocalDate nonNullDate = null;
		LocalDate localDate = LocalDate.now();
		try {
			if ( memberDate != null && !memberDate.equalsIgnoreCase("NULL")){
				nonNullDate = LocalDate.parse(memberDate);
			} else {
				nonNullDate = localDate;
			}
		} catch (DateTimeParseException e){
			throw e;
		}
		return nonNullDate;
	}

	
	public static boolean isDateBetween(LocalDate min, LocalDate max, LocalDate date){
	    return !(date.isBefore(min) || date.isAfter(max));
	}
	
	public static List<ProjectData> downloadHotelFile(String filePath) throws IOException {
		List<ProjectData> projects = new ArrayList<ProjectData>();
		File hotelFile = new File(filePath);
		if (hotelFile.exists()) {
			try (FileReader fileReader = new FileReader(hotelFile);
					BufferedReader in = new BufferedReader(fileReader);) {
				String s;
				String[] row;
				while ((s = in.readLine()) != null && !s.isEmpty()) {
					row = s.split(", ");
					ProjectData projectData = fillInData(row);
					projects.add(projectData);
				}
			} catch (IOException ex) {
				throw ex;
			}
		}
		return projects;
	}

	private static ProjectData fillInData(String[] row) {
		String name = null;
		String project = null;
		String fromDate = null;
		String toDate = null;
		for (int i = 0; i < row.length; i++) {
			String cell = row[i] == null ? null : row[i].trim();
			// the cell is not empty
			if (cell != null && !cell.isEmpty()) {
				switch (i) {
				case 0:
					name = row[i];
				case 1:
					project = row[i];
				case 2:
					fromDate = row[i];
				case 3:
					toDate = row[i];
				}
			}
		}
		ProjectData projectData = new ProjectData(name, project, fromDate, toDate);
		return projectData;
	}
}

class ProjectData {

	private String code;

	private String project;

	private String fromDate;

	private String toDate;

	public ProjectData(String code, String project, String fromDate, String toDate) {
		this.code = code;
		this.project = project;
		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(String toDate) {
		this.toDate = toDate;
	}

}

class CommonTimeProjectData {

	private String codeMember1;
	
	private String codeMember2;

	private String project;
	
	private long timeInProject;

	public CommonTimeProjectData(String codeMember1, String codeMember2, String project, long timeInProject2) {
		this.codeMember1 = codeMember1;
		this.codeMember2 = codeMember2;
		this.project = project;
		this.timeInProject = timeInProject2;
	}
	

	public String getCodeMember1() {
		return codeMember1;
	}


	public void setCodeMember1(String codeMember1) {
		this.codeMember1 = codeMember1;
	}


	public String getCodeMember2() {
		return codeMember2;
	}


	public void setCodeMember2(String codeMember2) {
		this.codeMember2 = codeMember2;
	}


	public String getProject() {
		return project;
	}


	public void setProject(String project) {
		this.project = project;
	}


	public long getTimeInProject() {
		return timeInProject;
	}


	public void setTimeInProject(long timeInProject) {
		this.timeInProject = timeInProject;
	}
}

