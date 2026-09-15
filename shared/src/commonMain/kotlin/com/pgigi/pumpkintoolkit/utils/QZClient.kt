package com.pgigi.pumpkintoolkit.utils

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.pgigi.pumpkintoolkit.AppConfig
import com.pgigi.pumpkintoolkit.models.Course
import com.pgigi.pumpkintoolkit.models.CoursePlan
import com.pgigi.pumpkintoolkit.models.ExamInfo
import com.pgigi.pumpkintoolkit.models.ExamScore
import com.pgigi.pumpkintoolkit.qzrc.preprocessImage
import com.pgigi.pumpkintoolkit.qzrc.recognizeCaptcha
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.encodeURLPath
import io.ktor.http.setCookie
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import com.pgigi.pumpkintoolkit.models.evaluation.EvaluationDetail
import com.pgigi.pumpkintoolkit.models.evaluation.EvaluationItem
import com.pgigi.pumpkintoolkit.models.evaluation.EvaluationListItem
import com.pgigi.pumpkintoolkit.models.evaluation.EvaluationMenuItem

object QZClient {
    val client by lazy {
        HttpClient {
            followRedirects = false
        }
    }
    
    var username = ""
    var password = ""

    var loginRedirectUrl = ""
    var logged = false

    val cookies = mutableListOf<Cookie>()

    private val loginSuccessCallbackList = mutableListOf<(String)->Unit>()

    // 设置回调
    fun addLoginSuccessCallback(callback: (jwQuickUrl: String) -> Unit) {
        this.loginSuccessCallbackList.add(callback)
    }

    // 移除回调
    fun removeLoginSuccessCallback(callback: (jwQuickUrl: String) -> Unit) {
        loginSuccessCallbackList.remove(callback)
    }

    private fun doLoginSuccessCallback(jwQuickUrl: String){
        loginSuccessCallbackList.forEach {
            it.invoke(jwQuickUrl)
        }
    }


    private fun List<Cookie>.toHeaderString() : String {
        return this.joinToString("; ") { "${it.name}=${it.value}" }
    }
    private fun url(path: String): String {
        return "${AppConfig.serverUrl.trimEnd('/')}/${path.trimStart('/')}"
    }

    fun replaceUrlOriginPure(oldFullUrl: String, newBaseUrl: String): String {
        // 捕获：协议://host[:port]，后面所有(path+query+fragment)分组保留
        val regex = Regex("""^(\w+://[^/]+)(/.*)?$""")
        val match = regex.matchEntire(oldFullUrl) ?: return oldFullUrl
        val suffix = match.groups[2]?.value ?: ""
        val base = newBaseUrl.trimEnd('/')
        return base + suffix
    }


    suspend fun login(onFailure: (reason: String) -> Unit = {}, onSuccess: (jwQuickUrl: String) -> Unit = {}): Boolean {
        try{
            val cookieList = mutableListOf<Cookie>()
            var response = client.get(url("verifycode.servlet"))
            if (response.status != HttpStatusCode.OK) {
                onFailure("获取图片验证码失败,请检查网络")
                return false
            }
            cookieList.addAll(response.setCookie())
            val verifyCode = recognizeCaptcha(preprocessImage(response.bodyAsBytes()))

            println(verifyCode)

            if (verifyCode.isEmpty()) {
                onFailure("获取图片验证码失败,请检查网络")
                return false
            }
            response = client.post(url("Logon.do?method=logon")){
                header(HttpHeaders.Cookie, cookieList.toHeaderString())
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                setBody((
                        "userAccount=${username}" +
                                "&userPassword=${password}" +
//                "&encoded=${Base64.encode(username.encodeToByteArray())}%%%" +
//                            Base64.encode(password.encodeToByteArray()) +
                                "&RANDOMCODE=$verifyCode")
                    .encodeURLPath()
                )
            }
            if (response.status != HttpStatusCode.Found) {
                val html = response.bodyAsText()
                val doc: Document = Ksoup.parse(html)
                val error = doc.select("#showMsg").first()?.text() ?: "未知错误"
                onFailure(error)
                return false
            }
            val location = replaceUrlOriginPure(response.headers[HttpHeaders.Location]?:"",
                AppConfig.serverUrl)
            if (location.trim { it.isWhitespace() }.isEmpty()) {
                onFailure("请联系开发者")
                println("1")
                return false
            }
            response = client.get(location) {
                header(HttpHeaders.Cookie, cookieList.toHeaderString())
            }
            if(response.status != HttpStatusCode.Found){
                onFailure("请联系开发者: Code="+response.status)
                return false
            }
            if(response.setCookie().isEmpty()){
                onFailure("获取登录凭证失败，请联系开发者")
                return false
            }
            cookieList.addAll(response.setCookie())
            cookies.clear()
            cookies.addAll(cookieList)
            loginRedirectUrl = location
            logged = true
            doLoginSuccessCallback(location)
            onSuccess(location)
            return true
        }catch (e: Exception){
            when(e){
                is ConnectTimeoutException -> {
                    onFailure("连接超时，请检查网络或教务系统")
                    return false
                }
                is SocketTimeoutException -> {
                    onFailure("连接超时，请检查网络或教务系统")
                    return false
                }
                is HttpRequestTimeoutException -> {
                    onFailure("连接超时，请检查网络或教务系统")
                    return false
                }
                else -> {
                    onFailure("未知错误，请检查网络或教务系统\n${e.message}")
                    return false
                }
            }
        }
    }

    suspend fun getHtml(url: String): String?{
        try{
            var retry = 0
            while(retry<=3){
                if(!logged) login(
                    { retry++ })
                if(logged){
                    val html = client.get(url) {
                        header(HttpHeaders.Cookie, cookies.toHeaderString())
                    }.bodyAsText()
                    val doc = Ksoup.parse(html)
                    if(doc.title().trim()=="登录") {
                        logged = false
                        login({ retry++ })
                        continue
                    }
                    return html
                }
            }
            return null
        }catch (_: Exception){
            return null
        }
    }

    fun parseCourses(html: String): List<Course>? {
        if (html.trim().isEmpty()) return null
        val courses = mutableListOf<Course>()
        val doc = Ksoup.parse(html.replace("&nbsp;",""))
        doc.select("span").remove()
        val table = doc.select("#kbtable tbody")
        try{
            val trs = table.select("tr")
            for(i in 1..5){
                val tds = trs[i].select("td")
                for(j in 0..6){
                    val td = tds[j].selectFirst("div.kbcontent") ?: continue
                    if(td.html().isEmpty()) continue

                    val courseHtmlList = td.html().split("---------------------")
                    for(courseHtml in courseHtmlList){
                        val courseDoc = Ksoup.parse(courseHtml)
                        val teacher = courseDoc.selectFirst("font[title=老师]")?.text() ?: ""
                        val classroom = courseDoc.selectFirst("font[title=教室]")?.text() ?: ""
                        val time = courseDoc.selectFirst("font[title=周次(节次)]")?.text() ?: ""
                        val weeks = time.substringBefore("(")
                        courseDoc.select("br").remove()
                        courseDoc.select("font").remove()
                        courseDoc.select("a").remove()
                        val courseName = courseDoc.text()
                            .replace("（", "(")
                            .replace("）", ")")
                            .trim()

                        val course = Course(
                            name = courseName,
                            classroom = classroom,
                            teacher = teacher,
                            weeks = weeks,
                            dayOfWeek = j,
                            lessonOfDay = 2*i-1
                        )
                        courses.add(course)
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
            return null
        }
        return courses
    }
    fun parseExamScores(html: String): List<ExamScore>? {
        if (html.trim().isEmpty()) return null
        val doc = Ksoup.parse(html)
        val resElements = doc.select("table#dataList tbody tr")
        val results = mutableListOf<ExamScore>()
        if (resElements.isEmpty()) return results
        for (elem in resElements) {
            val tds = elem.select("td")
            if (tds.size < 10) continue
            val name = tds[3].html()
            val score = tds[5].html()
            val credit: Float
            val totalClassHours: Int
            val gradePoint: Float
            try {
                credit = tds[7].html().toFloat()
                totalClassHours = tds[8].html().toInt()
                gradePoint = tds[9].html().toFloat()
            } catch (_: Exception) {
                continue
            }
            results.add(ExamScore(name, credit, gradePoint, score, totalClassHours))
        }
        return results
    }
    fun parseExperimentCourses(html:String):List<Course>?{
        if(html.trim{ it.isWhitespace()}.isEmpty()) return null
        val courses = mutableListOf<Course>()
        val doc = Ksoup.parse(html.replace("&nbsp;", " "))
        val table = doc.select("table#tblHead tbody tr")
        if (table.isEmpty()) return courses
        for(i in 1..<table.size){
            val tdElements = table[i].select("td")
            val startJ = if (i % 5 == 1) 2 else 1
            if(tdElements.size < startJ + 7) continue
            val week = (i + 4) / 5
            for(j in startJ..<startJ + 7){
                val td = tdElements[j]
                if(!td.html().contains("<br>")) continue
                val texts = td.html()
                    .replace("\n","")
                    .replace("<br>","\n")
                    .trim().split("\n".toRegex())
                val course = Course(
                    name = texts[0],
                    teacher = "",
                    classroom = texts[1].split(" ")[1],
                    weeks = week.toString(),
                    dayOfWeek = j - startJ,
                    lessonOfDay = (i%5)*2-1,
                    duration = 2
                )
                courses.add(course)
            }
        }
        return courses
    }
    suspend fun getEmptyRoomsHtml(termId: String, buildingId: String, day: Int, week: Int, lesson: Int): String? {
        return getHtml(
            url("jsxsd/kbcx/kbxx_classroom_ifr") +
                    "?xnxqh=${termId}" +
                    "&jzwid=${buildingId}" +
                    "&zc1=${week}&zc2=${week}" +
                    "&skxq1=${day}&skxq2=${day}" +
                    "&jc1=${lesson}&jc2=${lesson}"
        )
    }
    suspend fun getCoursesHtml(termId: String = ""): String? {
        return getHtml(url("jsxsd/xskb/xskb_list.do?xnxq01id=$termId"))
    }
    suspend fun getExperimentCoursesHtml(termId: String = ""): String? {
        return getHtml(url("jsxsd/syjx/toXskb.do?xnxq01id=$termId"))
    }
    suspend fun getExamScoreHtml(termId: String = "", display: String="all"): String? {
        return getHtml(url("jsxsd/kscj/cjcx_list?kksj=${termId}&xsfs=${display}"))
    }

    suspend fun getScheduleHtml(termId: String = ""): String?{
        return getHtml(url("jsxsd/jxzl/jxzl_query?xnxq01id=$termId"))
    }

    suspend fun getAllCourses(termId: String = ""): List<Course>?{
        val courses = mutableListOf<Course>()
        val cHtml = getCoursesHtml(termId)
        val eHtml = getExperimentCoursesHtml(termId)
        if(cHtml.isNullOrEmpty() || eHtml.isNullOrEmpty()) return null
        val cCourses = parseCourses(cHtml)
        val eCourses = parseExperimentCourses(eHtml)
        if(cCourses == null || eCourses == null) return null
        courses.addAll(cCourses)
        courses.addAll(eCourses)
        return courses
    }

    fun parseStartDate(html: String): LocalDate?{
        try{
            val doc = Ksoup.parse(html)
            val trElements = doc.select("#kbtable tbody tr")
            if (trElements.size <= 2) return null
            for(i in 1..7){
                val startTimeStr = trElements[1].select("td")[i].attr("title")
                if (startTimeStr.isBlank()) continue
                val format = LocalDate.Format {
                    year(Padding.ZERO)
                    char('年')
                    monthNumber(Padding.ZERO)
                    char('月')
                    day(Padding.ZERO)
                }
                return LocalDate.parse(startTimeStr, format)
            }
            return null
        }catch(e: Exception){
            return null
        }
    }

    suspend fun getStartDate(termId: String = ""): LocalDate?{
        val html = getScheduleHtml(termId)
        if (html.isNullOrBlank()) return null
        return parseStartDate(html)
    }

    fun parseWeekNum(html: String): Int{
        val doc = Ksoup.parse(html)
        val trElements = doc.select("#kbtable tbody tr")
        return if(trElements.size <= 2) -1
        else trElements.size - 2
    }

    suspend fun getWeekNum(termId: String = ""): Int?{
        val html = getScheduleHtml(termId)
        if (html.isNullOrBlank()) return null
        return parseWeekNum(html)

    }

    fun parseScheduleComment(html: String): String{
        try{
            val doc = Ksoup.parse(html)
            val trElements = doc.select("#kbtable tbody tr")
            if (trElements.size <= 2) return ""
            return trElements[1].select("td")[8].text()
        }catch(e: Exception){
            return ""
        }
    }

    suspend fun getScheduleComment(termId: String): String{
        val html = getScheduleHtml(termId)
        if (html.isNullOrBlank()) return ""
        return parseScheduleComment(html)
    }

    fun parseTerms(html: String): Map<String, String>? {
        if (html.trim().isEmpty()) return null
        val terms = LinkedHashMap<String, String>()
        val doc = Ksoup.parse(html)
        val options = doc.select("#xnxq01id option")
        if (options.isEmpty()) return null
        options.forEach { option ->
            terms[option.attr("value").trim { it.isWhitespace() }] = option.text().trim { it.isWhitespace() }
        }
        return terms
    }

    suspend fun getTermValueMap() : Map<String, String>?{
        val html = getCoursesHtml()
        if (html.isNullOrBlank()) return null
        return parseTerms(html)
    }

    fun parseCurrentTermValue(html: String): String? {
        if (html.trim().isEmpty()) return null
        val doc = Ksoup.parse(html)
        val currentTerm =
            doc.select("#xnxq01id option[selected]").attr("value").trim { it.isWhitespace() }
        return currentTerm.ifEmpty { null }
    }

    suspend fun getDefaultTermId() : String?{
        val html = getCoursesHtml()
        if (html.isNullOrBlank()) return null
        return parseCurrentTermValue(html)
    }

    fun parseEmptyRooms(html: String): Map<String, Boolean>? {
        if (html.trim().isEmpty()) return null
        val emptyRooms = mutableMapOf<String, Boolean>()
        val doc = Ksoup.parse(html.replace("&nbsp;".toRegex(), ""))
        val trElements = doc.select("#kbtable tbody tr")
        if(trElements.isEmpty()) {
            return emptyRooms
        }
        loop@ for (tr in trElements) {
            val tdElements = tr.select("td")
            var roomName = ""
            tdElements.forEachIndexed { index, td ->
                if(index == 0) {
                    roomName = td.text().trim().replace("（", "(").replace("）", ")")
                    emptyRooms[roomName] = true
                }else{
                    if(td.text().trim().isNotEmpty()&&roomName.isNotEmpty()) {
                        emptyRooms[roomName] = false
                        continue@loop
                    }
                }
            }
        }
        return emptyRooms
    }

    suspend fun getEmptyRooms(termId: String, buildingId: String, day: Int, week: Int, lesson: Int): Map<String, Boolean>?{
        val html = getEmptyRoomsHtml(termId,buildingId,day,week,lesson)
        if (html.isNullOrBlank()) return null
        return parseEmptyRooms(html)
    }

    suspend fun getCoursePlan(): List<CoursePlan>? {
        val html = getHtml(url("jsxsd/pyfa/pyfa_query"))
        if (html.isNullOrEmpty()) return null
        val doc = Ksoup.parse(html)
        val trElements = doc.select("table#dataList tbody tr")
        if (trElements.isEmpty()) return null
        val plans = mutableListOf<CoursePlan>()
        trElements.forEachIndexed { index, tr ->
            if(index != 0) {
                try{
                    val tdElements = tr.select("td")
                    val plan = CoursePlan(
                        tdElements[3].text().trim(),
                        tdElements[1].text().trim(),
                        tdElements[2].text().trim(),
                        tdElements[4].text().trim(),
                        tdElements[5].text().trim().toFloat(),
                        tdElements[6].text().trim().toInt(),
                        tdElements[7].text().trim(),
                        tdElements[8].text().trim(),
                        tdElements[9].text().trim(),
                        tdElements[10].text().trim()
                    )
                    plans.add(plan)
                }catch (_: Exception){ }
            }
        }
        return if (plans.isEmpty()) null else plans
    }

    fun parseExams1(html: String): List<ExamInfo> {
        val doc: Document = Ksoup.parse(html)
        return doc.select("table#dataList tr")
            .mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size < 9) return@mapNotNull null
                ExamInfo(
                    courseName = cells[2].text().trim(),
                    examWeek = cells[3].text().trim().toIntOrNull() ?: 0,
                    examDayOfWeek = cells[4].text().trim().toIntOrNull() ?: 0,
                    examTimeRaw = cells[5].text().trim(),
                    examLocation = cells[6].text().trim(),
                    campus = cells[7].text().trim(),
                    seatNumber = cells[8].text().trim().toIntOrNull(),
                )
            }
    }

    fun parseExams2(html: String): List<ExamInfo> {
        val doc: Document = Ksoup.parse(html)
        return doc.select("table#dataList tr")
            .mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size < 9) return@mapNotNull null
                ExamInfo(
                    courseName = cells[2].text().trim(),
                    examWeek = cells[3].text().trim().toIntOrNull() ?: 0,
                    examDayOfWeek = cells[4].text().trim().toIntOrNull() ?: 0,
                    examTimeRaw = cells[6].text().trim(),
                    examLocation = cells[7].text().trim(),
                    campus = cells[8].text().trim(),
                    seatNumber = null,
                )
            }
    }

    suspend fun getExams(termId: String = ""): List<ExamInfo>? {
        val h1 = getHtml(url("jsxsd/xsks/xsksap_list?xnxqid=$termId"))
        val h2 = getHtml(url("jsxsd/xsks/xsstk_list.do?xnxqid=$termId"))
        if (h1.isNullOrBlank() || h2.isNullOrBlank()) return null
        val list = mutableListOf<ExamInfo>()
        list.addAll(parseExams1(h1))
        list.addAll(parseExams2(h2))

        return list
    }

    suspend fun getExamScores(termId: String = ""): List<ExamScore>?{
        val html = getExamScoreHtml(termId)
        if (html.isNullOrBlank()) return null
        return parseExamScores(html)
    }

    fun parseEvaluationMenuItems(html: String): List<EvaluationMenuItem>?{
        if(html.isBlank()) return null
        val list = mutableListOf<EvaluationMenuItem>()
        val doc = Ksoup.parse(html)
        val trs = doc.select("table tbody tr")

        if(trs.size<=1) return list

        for (i in 1..<trs.size){
            val tds = trs[i].select("td")
            if(tds.size<7) continue
            val a = tds[6].selectFirst("a")
            a?.let {
                val url = a.attr("href")
                if(url.isNotBlank()){
                    list.add(EvaluationMenuItem(
                        termName = tds[1].text(),
                        evaluationName = tds[3].text(),
                        actionUrl = url
                    ))
                }
            }
        }
        return list
    }

    fun parseEvaluationListItems(html:String): List<EvaluationListItem>?{
        if(html.isBlank()) return null
        val list = mutableListOf<EvaluationListItem>()
        val doc = Ksoup.parse(html)
        val trs = doc.select("table tbody tr")

        if(trs.size<=1) return list

        for (i in 1..<trs.size){
            val tds = trs[i].select("td")
            if(tds.size<16) continue
            val a = tds[15].selectFirst("a")
            a?.let {
                val url = a.attr("href")
                if(url.isNotBlank()){
                    list.add(EvaluationListItem(
                        courseName = tds[2].text(),
                        teacher = tds[3].text(),
                        score = tds[5].text().toFloat(),
                        isSubmit = tds[7].text().trim().contentEquals("是"),
                        actionUrl = url,
                    ))
                }
            }
        }
        return list
    }

    fun parseEvaluationListTotalPage(html: String): Int?{
        val doc = Ksoup.parse(html)
        val element = doc.selectFirst(".Nsb_r_list_fy3")
        element?.let {
            val pattern = Regex("共(\\d+)页")
            val matchResult = pattern.find(element.text())
            return matchResult?.groupValues?.get(1)?.toIntOrNull()
        }
        return null
    }

    fun parseEvaluationDetail(html: String): EvaluationDetail?{
        val doc = Ksoup.parse(html)
        val inputs = doc.select("input")

        val keyValueMap = mutableMapOf<String, String>()
        val evaluationIdSet = mutableSetOf<String>()

        inputs.forEach {
            if(
                it.attr("name").startsWith("pj0601id_") &&
                it.attr("checked")!="checked"
            ) return@forEach
            if(it.attr("name").startsWith("pj0601fz_")) return@forEach
            if(it.attr("name")=="pj06xh") {
                evaluationIdSet.add(it.attr("value"))
                return@forEach
            }
            keyValueMap[it.attr("name")] = it.attr("value")
        }

        val trs = doc.select("table#table1 tbody tr")
        val evaluationList = mutableListOf<EvaluationItem>()

        if(trs.size<=1) return null

        for(i in 1..<trs.size){
            val tds = trs[i].select("td")
            if (tds.size < 2) continue
            if (tds[1].attr("name")!="zbtd") continue
            val title = tds[0].text().replace(" ","")

            val inputs = tds.select("input")
            var evaluationId : String? = null
            val scoreKeyValueMap = mutableMapOf<String, Float>()

            inputs.forEach {
                if(it.attr("name")=="pj06xh") evaluationId = it.attr("value")
            }
            evaluationId?.let {
                inputs.forEach {
                    if(it.attr("name").startsWith("pj0601fz_")){
                        val key = it.attr("name").removePrefix("pj0601fz_${evaluationId}_")
                        val value = it.attr("value").toFloat()
                        scoreKeyValueMap[key] = value
                    }
                }
                evaluationList.add(
                    EvaluationItem(
                        title = title,
                        evaluationId = evaluationId,
                        scoreKeyValueMap = scoreKeyValueMap
                    )
                )
            }
        }

        val commentElement = doc.selectFirst("textarea#jynr")
        val comment = if(commentElement!==null) commentElement.text() else ""

        return EvaluationDetail(
            keyValueMap = keyValueMap,
            evaluationIdSet = evaluationIdSet,
            evaluationList = evaluationList,
            comment = comment
        )
    }

    suspend fun getEvaluationMenuHItems(): List<EvaluationMenuItem>?{
        try{
            val h1 = client.get(url("jsxsd/xspj/xspj_find.do")) {
                header(HttpHeaders.Cookie, cookies.toHeaderString())
            }.bodyAsText()
            return parseEvaluationMenuItems(h1)
        }catch (e: Exception){
            return null
        }
    }

    suspend fun getEvaluationListItems(actionUrl: String): List<EvaluationListItem>?{
        try {
            val html = client.get(url(actionUrl)) {
                header(HttpHeaders.Cookie, cookies.toHeaderString())
            }.bodyAsText()
            if(html.isBlank()) return null
            val pageSize = parseEvaluationListTotalPage(html)

            pageSize?.let{
                val list = mutableListOf<EvaluationListItem>()
                for (i in 1..pageSize) {
                    val html =client.post(url(actionUrl)) {
                        header(HttpHeaders.Cookie, cookies.toHeaderString())
                        setBody(FormDataContent(Parameters.build {
                            append("pageIndex", i.toString())
                        }))
                    }.bodyAsText()
                    val l = parseEvaluationListItems(html)
                    l?.let {
                        list.addAll(l)
                    }
                }
                return list
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun getEvaluationDetail(actionUrl: String): EvaluationDetail?{
        try {
            val html = client.get(url(actionUrl)) {
                header(HttpHeaders.Cookie, cookies.toHeaderString())
            }.bodyAsText()
            if(html.isBlank()) return null
            return parseEvaluationDetail(html)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 提交评教。
     *
     * 向 jsxsd/xspj/xspj_save.do 发送 POST 请求，表单参数构建规则：
     * 1. 以 [EvaluationDetail.keyValueMap] 为基础
     * 2. 将 issubmit 的值改为 "1"
     * 3. pj0601id_ 前缀的参数根据 [selectedValues] 中用户的选择复写
     * 4. [EvaluationDetail.evaluationIdSet] 中每个值都作为 pj06xh 参数添加（允许多个同名 key）
     * 5. 追加 jynr = [comment]
     *
     * @return 响应中 alert(...) 的消息文本，网络异常时返回 null
     */
    suspend fun submitEvaluation(
        detail: EvaluationDetail,
        selectedValues: Map<String, String>,
        comment: String
    ): String? {
        try {
            val params = Parameters.build {
                detail.keyValueMap.forEach { (key, value) ->
                    if (!key.startsWith("pj0601id_") && key != "issubmit") {
                        append(key, value)
                    }
                }
                append("issubmit", "1")
                detail.evaluationList.forEach { item ->
                    val selected = selectedValues[item.evaluationId]
                    if (!selected.isNullOrEmpty()) {
                        append("pj0601id_${item.evaluationId}", selected)
                    }
                }
                detail.evaluationIdSet.forEach { id ->
                    append("pj06xh", id)
                }
                append("jynr", comment)
            }
            val html = client.post(url("jsxsd/xspj/xspj_save.do")) {
                header(HttpHeaders.Cookie, cookies.toHeaderString())
                setBody(FormDataContent(params))
            }.bodyAsText()
            val pattern = Regex("alert\\('([^']*)'\\)")
            return pattern.find(html)?.groupValues?.get(1)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 自动评教：所有子项选最高分，随机一项选第二高分，评语留空。
     *
     * @param actionUrl 评教详情页的 actionUrl（来自列表项）
     * @return 提交结果消息，如"保存成功"；获取详情失败或网络异常返回 null
     */
    suspend fun autoEvaluate(actionUrl: String): String? {
        val detail = getEvaluationDetail(actionUrl) ?: return null
        val selectedValues = mutableMapOf<String, String>()
        val sortedItems = detail.evaluationList.filter { it.scoreKeyValueMap.isNotEmpty() }
        if (sortedItems.isEmpty()) return null
        val secondIndex = sortedItems.indices.random()
        sortedItems.forEachIndexed { index, item ->
            val sorted = item.scoreKeyValueMap.entries.sortedByDescending { it.value }
            selectedValues[item.evaluationId] = if (index == secondIndex && sorted.size > 1) {
                sorted[1].key
            } else {
                sorted[0].key
            }
        }
        return submitEvaluation(detail, selectedValues, "")
    }

}