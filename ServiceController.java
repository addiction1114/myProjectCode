package dev.mountaingo.kr.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.view.RedirectView;

import dev.mountaingo.kr.define_object.lucy.LucyXss;
import dev.mountaingo.kr.model.CalInfo;
import dev.mountaingo.kr.model.CalMain;
import dev.mountaingo.kr.model.Codes;
import dev.mountaingo.kr.model.SanAddress;
import dev.mountaingo.kr.model.SanGroupInfo;
import dev.mountaingo.kr.model.SanGroupMember;
import dev.mountaingo.kr.model.Seat;
import dev.mountaingo.kr.model.SeatInfo;
import dev.mountaingo.kr.model.User;
import dev.mountaingo.kr.service.interfaces.Service;


@Controller
@RequestMapping("/service")
public class ServiceController extends MultiActionController{
		
	@Autowired
	private Service service;
	
	@Resource(name="lucy")
	private LucyXss lucyXss;
	
	
	@RequestMapping(value="/openSearch")
	public String openSearch(HttpServletRequest request, @RequestParam Map<String, Object> map){
		map.clear();
		List<SanAddress> saNameList = service.selectSanAddress(map);
		request.setAttribute("saNameList", saNameList);
		return "/search/search";
	}
	@RequestMapping(value="/getSearchSanAddress")
	public ModelAndView getSearchSanAddress(HttpServletRequest request, @RequestParam Map<String, Object> map){
		List<SanAddress> saList = service.selectSanAddress(map);
		ModelAndView view = new ModelAndView();
		view.addObject("saList", saList);
		view.setViewName("jsonView");
		
		return view;
	}

	@RequestMapping(value="/findAddress", method=RequestMethod.POST)
	public ModelAndView selectSanAddress(
			HttpServletRequest request,
			@RequestParam Map<String, Object> map){
		
		List<SanAddress> list = service.selectSanAddress(map);
		ModelAndView view = new ModelAndView();
		view.addObject("list",list);
		view.setViewName("jsonView");
		
		return view;
	}
	
	@RequestMapping(value="/registerCommunity")
	public ModelAndView registerCommunity(
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam Map<String, String> map){
		
		ModelAndView view = new ModelAndView();
		view.setViewName("/service/registerCommunity");
		
		return view;
	}

	@RequestMapping(value="/insertCommunity",method = RequestMethod.POST)
	public String insertCommunity(
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam Map<String, String> map) throws UnsupportedEncodingException{
		
		map = lucyXss.lucyScriptString(map);
		service.insertCommunity(map);
		
		return "/service/registerCommunity";
	}
	
	@RequestMapping(value="/removeCommunity",method = RequestMethod.POST)
	public String removeCommunity(
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam Map<String, String> map) throws UnsupportedEncodingException{
		
		service.deleteCommunity(map);
		
		return "/service/registerCommunity";
	}
	
	
	@RequestMapping(value="/updateCommunity",method = RequestMethod.POST)
	public String updateCommunity(
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam Map<String, String> map) throws UnsupportedEncodingException{
		map = lucyXss.lucyScriptString(map);
		service.updateCommunity(map);
		
		//커뮤니티도 등록되고 게시판 테이블 생성을 위한 CODES테이블에도 행삽입
		return "redirect:/service/registerCommunity";
	}
	
	
	@RequestMapping(value="/ajaxSelectCommunity")
	public ModelAndView ajaxSelectCommunity(
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value="name", required=false) String name, 
			@RequestParam Map<String, String> map){
		
		
		map.clear();
		if(name != null && !"".equals(name)){ //일반 파라미터 셋팅
			map.put("whereParam", "NAME");
			map.put("paramValue", name);
		}
		
		List<SanGroupInfo> list= service.selectCommunity(map);
		
		ModelAndView mav = new ModelAndView();
		mav.setViewName("jsonView");
		mav.addObject("infoList", list);
		
		return mav;
	}
	
	
	@RequestMapping(value="calendar/{sgiId}", method=RequestMethod.GET)
	public ModelAndView calendar(HttpServletRequest request, HttpServletResponse response,
									@PathVariable("sgiId") String sgiId,
									Map<String, String> map,
									Authentication auth){
		
		User user = translateUserFromAuth(auth);
		
		map.put("whereParam", "SGI_ID");
		map.put("paramValue", sgiId);
		List<SanGroupInfo> sgiInfo = service.selectCommunity(map);
		String name = sgiInfo.get(0).getMember().getName();
		if(sgiInfo.size() <= 0){
			map.clear();
			ModelAndView view1 = new ModelAndView();
			view1.setView(new RedirectView("/service/calendarSelect"));
			return view1;
		}
		else if(!name.equals(user.getName())){  //달력을 보는데 산행모임 등록자 이름과 로그인한 이름이 다르면 로그인한 회원이 따로 산행 모임에 가입한 회원인지 검사한다
			Map<String, Object> sgmMap = new HashMap<String, Object>();
			sgmMap.put("email", user.getEmail());
			List<SanGroupMember> list = service.selectSgm(sgmMap);
			
			boolean sgmFlag = false;
			for(SanGroupMember sgm : list){
				if(sgiId.equals(sgm.getSgi().getSgiId()) && "ACCEPT_Y".equals(sgm.getC().getCodeSeq())){
					sgmFlag = true;
					break;
				}
			}
			if(sgmFlag == false){
				map.clear();
				ModelAndView view2 = new ModelAndView();
				view2.setView(new RedirectView("/service/calendarSelect"));
				return view2;
			}
		}
		
		ModelAndView view = new ModelAndView();
		view.addObject("grade", "u");
		view.addObject("sgiId", sgiId);
		view.addObject("sgiName", sgiInfo.get(0).getSgiName());
		view.setViewName("service/calendar");
		return view;
	}
	
	@RequestMapping(value="calendar/{sgiId}", method=RequestMethod.POST)
	public ModelAndView calendar(HttpServletRequest request, HttpServletResponse response,
									@RequestParam("grade") String grade,
									@PathVariable("sgiId") String sgiId,
									@RequestParam("sgiName") String sgiName,
									Map<String, String> map){
		
		ModelAndView view = new ModelAndView();
		
		view.addObject("grade",grade);
		view.addObject("sgiId",sgiId);
		view.addObject("sgiName",sgiName);
		view.setViewName("service/calendar");
		
		return view;
	}
	
	
	@RequestMapping(value="calendarSelect")
	public ModelAndView calendarSelect(Authentication auth, HttpServletRequest request, HttpServletResponse response, HttpSession session, Map<String, String> map){
		
		User user = translateUserFromAuth(auth);
		String name = user.getName();

		map.put("whereParam", "NAME");
		map.put("paramValue", name);
		List<SanGroupInfo> availableList = service.selectCommunity(map);
		if(name.equals("") || name == null) {
			availableList.clear();
		}
		map.clear();
		//List<SanGroupInfo> allList = service.selectCommunity(map);
		
		
		ModelAndView view = new ModelAndView();
		view.addObject("availableList", availableList);
		//view.addObject("allList", allList);
		view.setViewName("service/calendar_flag");
		
		return view;
	}
	
	@RequestMapping(value="selectCommunityAjax")
	public ModelAndView selectComunityAjax(Authentication auth,
			HttpServletRequest request, HttpServletResponse response, HttpSession session, 
			@RequestParam Map<String, String> map,
			@RequestParam(value="joinSgm", required=false) String joinSgm){
		
		//Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		User user = translateUserFromAuth(auth);
		
		if(joinSgm != null && joinSgm.equals("yes") && user != null){
			map.put("email",user.getEmail());
			map.put("name",user.getName());
		}
		
		Set<String> s = map.keySet();
		Iterator<String> it = s.iterator();
		
		//정렬칼럼 
		while(it.hasNext()){
			String key = it.next();
			String mapValue = map.get(key);
			if(key.contains("sort") && mapValue != null && !mapValue.equals("")){
				int start = key.indexOf("[") + 1;
				int end = key.indexOf("]");
				String sortCol = key.substring(start, end).toUpperCase();
				int idx = sortCol.indexOf("SGI") + 3;
				StringBuffer sb = new StringBuffer(sortCol);
				sb.insert(idx, '_');
				map.put("sortCol", sb.toString());
				String sortVal = map.get(key);
				map.put("sortVal", sortVal);
				break;
			}
		}
		//map.clear();
		
		int current = Integer.parseInt(request.getParameter("current")); 
		int rowCount = Integer.parseInt(request.getParameter("rowCount"));

		int limitFirstValue = 0;
		if(current > 1){
			limitFirstValue = (current-1)*rowCount;
		}
		map.put("limitFirstValue", String.valueOf(limitFirstValue));
		List<SanGroupInfo> allList = service.selectCommunity(map);
		
		current = current > 1 ? current : 1;
		
		//rowCount = rowCount != null && !rowCount.equals("") ? rowCount : "10";
		//int total = service.selectCommunityCount();
		ModelAndView view = new ModelAndView();
		view.addObject("current", current);
		view.addObject("rowCount", rowCount);
		view.addObject("rows", allList);
		view.addObject("total", allList.size());
		view.setViewName("jsonView");
		
		return view;
	}
	
	
	@RequestMapping(value="insertCalendar", method = RequestMethod.POST)
	public ModelAndView insertCalendar(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, String> map){
		lucyXss.lucyScriptString(map);
		
		int result = service.insertCalendar(map);
		String message = "fail";
		ModelAndView view = new ModelAndView();
		
		if(result > 0){
			message = "success";
			view.addObject("result", message);
		}else{
			view.addObject("result", message);
		}
		view.setViewName("jsonView");
		
		return view;
	}

	@RequestMapping(value="selectCalendarJson", method = RequestMethod.POST)
	public ModelAndView selectCalendarJson(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, String> map){
		map = lucyXss.lucyScriptString(map);
		 List<CalInfo> jsonListCalendar  = service.selectCalendarJson(map);
		
		
		ModelAndView view = new ModelAndView();
		view.addObject("jsonListCalendar", jsonListCalendar);
		view.setViewName("jsonView");
		
		return view;
	}
	
	@RequestMapping(value="deleteCalendarEvent", method = RequestMethod.POST)
	public ModelAndView deleteCalendarEvent(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, String> map){
		
		map = lucyXss.lucyScriptString(map);
		String resultString = "fail";
		int result  = service.deleteCalendarEvent(map);
		
		if(result > 0){
			resultString ="success";
		}
		
		ModelAndView view = new ModelAndView();
		view.addObject("result", resultString);
		view.setViewName("jsonView");
		
		return view;
	}
	
	
	/* 산악회 회원 */
	@RequestMapping(value="joinManage")
	public ModelAndView memberManage(Authentication auth, @RequestParam Map<String, Object> map){
		
		ModelAndView view = new ModelAndView();
		view.setViewName("/service/joinManage");
		User user = translateUserFromAuth(auth);
		if(user == null) return view;
		
		//System.out.println(user.getEmail());
		map.put("email", user.getEmail());
		List<SanGroupMember> sgmList = service.selectSgm(map);
		
		view.addObject("sgmList", sgmList);
		view.addObject("sgmListSize",sgmList.size());
		
		return view;
	}
	
	
	@RequestMapping(value="joinSgm")
	public ModelAndView joinSgm(Authentication auth, @RequestParam Map<String, Object> map){
		int result = 0;
		User user = translateUserFromAuth(auth);
		String email = user.getEmail();
		  
		  if(email.equals("")){
			 result = 0;
		  }else{
			  map.put("email", email);
			  result = service.insertSgm(map);
		  }
		
		ModelAndView view = new ModelAndView();
		view.setViewName("jsonView");
		view.addObject("result",result);
		
		
		return view;
	}
	
	
	@RequestMapping(value="joinManager")
	public ModelAndView joinManager(Authentication auth, @RequestParam Map<String, Object> map, HttpServletResponse response){
		
		 response.setHeader("Pragma", "No-cache"); 
		 response.setDateHeader("Expires", 0); 
		 response.setHeader("Cache-Control", "no-Cache"); 
		
		ModelAndView view = new ModelAndView();
		view.setViewName("/service/joinManager");
		
		return view;
	}
	
	
	@RequestMapping(value="acceptSgm")
	public ModelAndView acceptSgm(Authentication auth, @RequestParam Map<String, Object> map){
	
		
		int result = service.acceptSgm(map);
		
		ModelAndView view = new ModelAndView();
		
		view.setViewName("jsonView");
		view.addObject("result",result);
		
		return view;
		
	}
	
	@RequestMapping(value="removeSgm")
	public ModelAndView removeSgm(Authentication auth, 
								  @RequestParam Map<String, Object> map,
								  @RequestParam(value="email", required=false) String email){
		ModelAndView view = new ModelAndView();
		view.setViewName("jsonView");
		
		if(email == null || email.equals("")){
			User user = translateUserFromAuth(auth);
			if(user == null){
				return view;
			}
			map.put("email", user.getEmail());
		}
		
		int result = service.removeSgm(map);
		
		
		view.addObject("result",result);
		
		return view;
		
	}
	
	
	@RequestMapping(value="selectManagerViewAjax")
	public ModelAndView selectManagerViewAjax(Authentication auth, HttpServletRequest request,
			@RequestParam Map<String, String> map){
		
		ModelAndView view = new ModelAndView();
		view.setViewName("jsonView");
		User user = translateUserFromAuth(auth);
		if(user == null) return view;
		
		map.put("name", user.getName());
		
		Set<String> s = map.keySet();
		Iterator<String> it = s.iterator();
		
		//정렬칼럼 
		while(it.hasNext()){
			String key = it.next();
			String mapValue = map.get(key);
			if(key.contains("sort") && mapValue != null && !mapValue.equals("")){
				int start = key.indexOf("[") + 1;
				int end = key.indexOf("]");
				String sortCol = key.substring(start, end).toUpperCase();
				int idx = sortCol.indexOf("SGI") + 3;
				StringBuffer sb = new StringBuffer(sortCol);
				sb.insert(idx, '_');
				map.put("sortCol", sb.toString());
				String sortVal = map.get(key);
				map.put("sortVal", sortVal);
				break;
			}
		}
		//map.clear();
		
		int current = Integer.parseInt(request.getParameter("current")); 
		int rowCount = Integer.parseInt(request.getParameter("rowCount"));

		int limitFirstValue = 0;
		if(current > 1){
			limitFirstValue = (current-1)*rowCount;
		}
		map.put("limitFirstValue", String.valueOf(limitFirstValue));
		List<SanGroupMember> allList = service.selectManagerViewAjax(map);
		
		current = current > 1 ? current : 1;
		
		view.addObject("current", current);
		view.addObject("rowCount", rowCount);
		view.addObject("rows", allList);
		view.addObject("total", allList.size());
		
		return view;
	}
	
	
	@RequestMapping(value="selectMySgmAjax")
	public ModelAndView selectMySgmAjax(Authentication auth, HttpServletRequest request,
			@RequestParam Map<String, Object> map){
		
		ModelAndView view = new ModelAndView();
		view.setViewName("jsonView");
		User user = translateUserFromAuth(auth);
		if(user == null) return view;
		
		map.put("email", user.getEmail());
		map.put("name", user.getName());
		 
		List<SanGroupMember> sgmList = service.selectMySgm(map);
		int sgmCount = service.selectMySgmTotal(map);
		
		view.addObject("sgmList",sgmList);
		view.addObject("sgmListSize",sgmCount);
		return view;
	}
	
	
	
	@RequestMapping(value="selectSeat")
	public ModelAndView seat(HttpServletRequest request, @RequestParam Map<String, String> map){
		
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String name = "";
		String email = "";
		if (principal instanceof User) {
		  name = ((User)principal).getName();
		  email = ((User)principal).getEmail();
		  if(!name.equals("")){
			  map.put("paramValue", name);
			  map.put("whereParam", "NAME");
		  }
		}
		
		Map<String, Object> seatViewMap = new HashMap();
		seatViewMap.put("email", email);
		seatViewMap.put("acceptFlag", "Y");
		seatViewMap.put("accept", "ACCEPT_Y");
		List<SanGroupMember> sgmList = service.selectSgm(seatViewMap);
		
		List<SanGroupInfo> list = service.selectCommunity(map);
		
		ModelAndView view = new ModelAndView();
		view.addObject("sgiList", list);
		view.addObject("sgmList", sgmList);
		view.setViewName("/service/selectSeat");
		
		return view;
	}
	
	@RequestMapping(value="makeSeat/{sgiId}")
	public ModelAndView seat(@PathVariable("sgiId") String sgiId,
								@RequestParam Map<String, String> map,
								Authentication auth){
		
		User user = translateUserFromAuth(auth);
		if(user == null) return new ModelAndView(new RedirectView("/home"));
		
		
		map.put("whereParam", "SGI_ID");
		map.put("paramValue", sgiId);
		List<SanGroupInfo> list = service.selectCommunity(map);
		SanGroupInfo sgi = null;
		
		if(list.size() > 0){
			sgi = list.get(0);
		}
		
		if(!sgi.getMember().getName().equals(user.getName())){
			return new ModelAndView(new RedirectView("/home"));
		}
		
		String siId = "SI_" + sgiId.substring(sgiId.indexOf("_")+1);
		map.put("siId", siId);
		SeatInfo si = service.selectSeatInfo(map);
		
		
		ModelAndView view = new ModelAndView();
		if(si != null){
			view.addObject("si",si);
			view.addObject("seatFlag","u");
		}else{
			view.addObject("seatFlag","i");
		}
			
		view.addObject("sgi", sgi);
		view.setViewName("/service/makeSeat");
		return view;
	}
	
	@RequestMapping(value="createSeat", method=RequestMethod.POST)
	public String createSeat(HttpServletResponse response,
			@RequestParam Map<String, Object> map,
			@RequestParam(value="seat",required=false) String seat,
			@RequestParam(value="sgiId",required=false) String sgiId){
		
		int idx = sgiId.indexOf("_");
		String siId = "SI_" + sgiId.substring(idx+1);
		map.put("siId", siId);
		//StringBuffer sb = null;
		List<Seat> list = new ArrayList<Seat>();
		if(seat != null && !seat.equals("")){
			String[] seatOne = seat.split(";");
			String[] seatNoName = null;
			Seat s = null;
			Map<String, Object> seatMap = new HashMap<String, Object>();
			for(int i = 0; i < seatOne.length; i++){
				seatNoName = seatOne[i].split("=");
				s = new Seat();
				s.setsNo(Integer.parseInt(seatNoName[0]));
				s.setsName(seatNoName[1]);
				//seatMap.put("sNo", seatNoName[0]);
				//seatMap.put("sName", seatNoName[1]);
				list.add(s);
				
			}
			map.put("seatInfo", list);
		}else{
			map.put("seatInfo", "");
		}
		int result = service.insertSeatData(map);
		
		if(result > 0){
			 response.setContentType("text/html; charset=euc-kr"); 
			 PrintWriter w;
			 String word = "좌석 배치 생성에 실패하였습니다. 관리자에게 문의하세요.";
			 String path = "../service/makeSeat/"+sgiId;
			try {
				w = response.getWriter();
				w.println("<script  charset='euc-kr'>");
				w.println("alert('"+word+"');");
				w.println("location.href ='"+path+"';");
				w.println("</script>");
				w.flush();
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		//ModelAndView view = new ModelAndView();
		//view.setView(new RedirectView("/service/selectSeat/"+sgiId));
		//view.setViewName("jsonView");
		
		
		return "redirect:/service/makeSeat/"+sgiId;
	}
	
	@RequestMapping(value="delSeatInfo", method=RequestMethod.POST)
	public String delSeat(@RequestParam Map<String, Object> map,
						@RequestParam("sgiId") String sgiId,
						HttpServletRequest request){
			
			String siId = "SI_" + sgiId.substring(sgiId.indexOf('_')+1);
			map.put("siId", siId);
			int result = service.deleteSeatAndInfo(map);
			
			//request.setAttribute("result", result);
			
			return "redirect:/service/selectSeat";
	}

	@RequestMapping(value="uptSeatName", method=RequestMethod.POST)
	public String uptSeatName(@RequestParam Map<String, Object> map,
			//@RequestParam("sgiId") String sgiId,
			@RequestParam("name") String sNo,
			@RequestParam("value") String sName,
			HttpServletRequest request){
		
		int idx = sNo.indexOf('_')+1;
		sNo = sNo.substring(idx);
		map.put("sNo", sNo);
		map.put("sName", sName);
		
		service.updateSeat(map);
		
		//request.setAttribute("result", result);
		
		return "redirect:/service/selectSeat";
	}
	
	@RequestMapping(value="viewSeat/{sgiId}")
	public String viewSeat(Authentication auth, @PathVariable("sgiId") String sgiId, 
							@RequestParam Map<String, Object> map, HttpServletRequest request){
		User user = translateUserFromAuth(auth);
		map.put("email", user.getEmail());
		map.put("acceptFlag", "Y");
		map.put("accept", "ACCEPT_Y");
		List<SanGroupMember> sgmList = service.selectSgm(map);
		
		boolean flag = containsSgiId(sgiId, sgmList);
		if(flag == false){
			return "redirect:/service/selectSeat";
		}
		
		Map<String, String> siMap = new HashMap<String, String>();
		siMap.put("sgiId", sgiId);
		int idx = sgiId.indexOf('_')+1;
		siMap.put("siId", "SI_" + sgiId.substring(idx));
		SeatInfo si = service.selectSeatInfo(siMap);
		
		request.setAttribute("seatInfo", si);
		
		return "service/viewSeat";
	}
	
	
	//Authentication객체를 User객체로 변환해서 리턴해주는 메서드
	private User translateUserFromAuth(Authentication auth){
		User user = (auth != null && auth.getPrincipal() instanceof User) ?  (User)auth.getPrincipal() : null;
		return user;
	}
	//파라미터로 넘어온 sgiId값이 디비에서 조회해온 sgiId값과 일치하는지를 리턴(파라미터, 디비조회값)인자
	private boolean containsSgiId(String sgiId, List<SanGroupMember> sgmList){
		boolean flag = false;
		for(SanGroupMember sgm : sgmList){
			if(sgiId.equalsIgnoreCase(sgm.getSgi().getSgiId())){
				flag = true;
				break;
			}
		}
		return flag;
	}
	
}
