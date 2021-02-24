package com.simplemobiletools.calendar.pro.views

import android.content.Context
import android.graphics.*
import android.mokee.utils.MoKeeUtils
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.View
import androidx.core.content.ContextCompat
import com.mokee.cloud.calendar.ChineseCalendar
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.getWHSharedPrefs
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.helpers.FLAG_HOLIDAY
import com.simplemobiletools.calendar.pro.helpers.FLAG_WORKDAY
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.pro.helpers.MEDIUM_ALPHA
import com.simplemobiletools.calendar.pro.helpers.isWeekend
import com.simplemobiletools.calendar.pro.models.DayMonthly
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.MonthViewEvent
import com.simplemobiletools.commons.extensions.*
import org.joda.time.DateTime
import org.joda.time.Days
import java.util.*

// used in the Monthly view fragment, 1 view per screen
class MonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private val BG_CORNER_RADIUS = 8f
    private val ROW_COUNT = 6

    private var textPaint: Paint
    private var eventTitlePaint: TextPaint
    private var gridPaint: Paint
    private var config = context.config
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var primaryColor = 0
    private var textColor = 0
    private var redTextColor = 0
    private var weekDaysLetterHeight = 0
    private var eventTitleHeight = 0
    private var currDayOfWeek = 0
    private var smallPadding = 0
    private var maxEventsPerDay = 0
    private var horizontalOffset = 0
    private var dayVerticalOffset = 0
    private var showWeekNumbers = false
    private var dimPastEvents = true
    private var highlightWeekends = false
    private var isPrintVersion = false
    private var isMonthDayView = false
    private var allEvents = ArrayList<MonthViewEvent>()
    private var bgRectF = RectF()
    private var dayLetters = ArrayList<String>()
    private var days = ArrayList<DayMonthly>()
    private var dayVerticalOffsets = SparseIntArray()
    private var isCN = false

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        primaryColor = context.getAdjustedPrimaryColor()
        textColor = config.textColor
        redTextColor = context.resources.getColor(R.color.red_text)
        showWeekNumbers = config.showWeekNumbers
        dimPastEvents = config.dimPastEvents
        highlightWeekends = config.highlightWeekends

        smallPadding = resources.displayMetrics.density.toInt()
        val normalTextSize = resources.getDimensionPixelSize(R.dimen.normal_text_size)
        dayVerticalOffset = (resources.getDimensionPixelSize(R.dimen.normal_text_size) * 0.15f).toInt()
        weekDaysLetterHeight = normalTextSize * 2

        isCN = MoKeeUtils.isSupportLanguage(false)

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = normalTextSize.toFloat()
            textAlign = if (isCN) Paint.Align.LEFT else Paint.Align.CENTER
        }

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.adjustAlpha(LOW_ALPHA)
        }

        val smallerTextSize = resources.getDimensionPixelSize(R.dimen.smaller_text_size)
        eventTitleHeight = smallerTextSize
        eventTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = smallerTextSize.toFloat()
            textAlign = Paint.Align.LEFT
        }

        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
    }

    fun updateDays(newDays: ArrayList<DayMonthly>, isMonthDayView: Boolean) {
        this.isMonthDayView = isMonthDayView
        days = newDays
        showWeekNumbers = config.showWeekNumbers
        horizontalOffset = if (showWeekNumbers) eventTitleHeight * 2 else 0
        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
        groupAllEvents()
        invalidate()
    }

    private fun groupAllEvents() {
        days.forEach {
            val day = it
            day.dayEvents.forEach {
                val event = it

                // make sure we properly handle events lasting multiple days and repeating ones
                val lastEvent = allEvents.lastOrNull { it.id == event.id }
                val daysCnt = getEventLastingDaysCount(event)
                val validDayEvent = isDayValid(event, day.code)
                if ((lastEvent == null || lastEvent.startDayIndex + daysCnt <= day.indexOnMonthView) && !validDayEvent) {
                    val monthViewEvent = MonthViewEvent(event.id!!, event.title, event.startTS, event.color, day.indexOnMonthView,
                        daysCnt, day.indexOnMonthView, event.getIsAllDay(), event.isPastEvent)
                    allEvents.add(monthViewEvent)
                }
            }
        }

        allEvents = allEvents.asSequence().sortedWith(compareBy({ -it.daysCnt }, { !it.isAllDay }, { it.startTS }, { it.startDayIndex }, { it.title }))
            .toMutableList() as ArrayList<MonthViewEvent>
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dayVerticalOffsets.clear()
        measureDaySize(canvas)

        if (config.showGrid && !isMonthDayView) {
            drawGrid(canvas)
        }

        addWeekDayLetters(canvas)
        if (showWeekNumbers && days.isNotEmpty()) {
            addWeekNumbers(canvas)
        }

        var curId = 0
        for (y in 0 until ROW_COUNT) {
            for (x in 0..6) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    dayVerticalOffsets.put(day.indexOnMonthView, dayVerticalOffsets[day.indexOnMonthView] + weekDaysLetterHeight)
                    val verticalOffset = dayVerticalOffsets[day.indexOnMonthView]
                    val xPos = x * dayWidth + horizontalOffset
                    val yPos = y * dayHeight + verticalOffset
                    val xPosCenter = xPos + dayWidth / 2
                    var xPosLeft = xPosCenter - dayWidth / 3
                    var xPosRight = xPosCenter - dayWidth / 16
                    if (day.isToday && !isPrintVersion) {
                        var xPosCircle = 0f
                        if (day.value.toString().length > 1) {
                            xPosCircle = xPosLeft + textPaint.textSize / 1.75f
                        } else {
                            xPosCircle = xPosLeft + textPaint.textSize / 3.5f
                        }
                        canvas.drawCircle(if (isCN) xPosCircle else xPosCenter, yPos + textPaint.textSize * 0.65f, textPaint.textSize * 0.65f, getCirclePaint(day))
                    }
                    if (isCN) {
                        canvas.drawText(day.value.toString(), xPosLeft, yPos + textPaint.textSize, getTextPaint(day))
                        var dateTime = Formatter.getDateTimeFromCode(day.code)
                        var cal = Calendar.getInstance();
                        cal.set(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth)
                        var chineseCalendarInfo = ChineseCalendar(cal).chineseCalendarInfo
                        var suggestDay = if (TextUtils.isEmpty(chineseCalendarInfo.solarTerm)) chineseCalendarInfo.lunarDay else chineseCalendarInfo.solarTerm
                        canvas.drawText(suggestDay, xPosRight, yPos + textPaint.textSize, getLunarTextPaint(day))
                        var date = dateTime.year.toString() + "-" + dateTime.monthOfYear + "-" + dateTime.dayOfMonth
                        var flag = context.getWHSharedPrefs().getInt(date, 0)

                        val whPath = Path()
                        whPath.moveTo(xPos + dayWidth - 20, yPos)
                        whPath.lineTo(xPos + dayWidth, yPos + 20)
                        whPath.lineTo(xPos + dayWidth, yPos)
                        whPath.close()
                        if (flag == FLAG_HOLIDAY) {
                            canvas.drawPath(whPath, getColoredPaint(ContextCompat.getColor(context, android.R.color.holo_orange_dark)))
                        } else if (flag == FLAG_WORKDAY) {
                            canvas.drawPath(whPath, getColoredPaint(ContextCompat.getColor(context, android.R.color.darker_gray)))
                        }
                    } else {
                        canvas.drawText(day.value.toString(), xPosCenter, yPos + textPaint.textSize, getTextPaint(day))
                    }
                    dayVerticalOffsets.put(day.indexOnMonthView, (verticalOffset + textPaint.textSize * 2).toInt())
                }
                curId++
            }
        }

        if (!isMonthDayView) {
            for (event in allEvents) {
                drawEvent(event, canvas)
            }
        }
    }

    private fun drawGrid(canvas: Canvas) {
        // vertical lines
        for (i in 0..6) {
            var lineX = i * dayWidth
            if (showWeekNumbers) {
                lineX += horizontalOffset
            }
            canvas.drawLine(lineX, 0f, lineX, canvas.height.toFloat(), gridPaint)
        }

        // horizontal lines
        canvas.drawLine(0f, 0f, canvas.width.toFloat(), 0f, gridPaint)
        for (i in 0..5) {
            canvas.drawLine(0f, i * dayHeight + weekDaysLetterHeight, canvas.width.toFloat(), i * dayHeight + weekDaysLetterHeight, gridPaint)
        }
    }

    private fun addWeekDayLetters(canvas: Canvas) {
        for (i in 0..6) {
            val xPos = horizontalOffset + (i + 1) * dayWidth - dayWidth / 2
            var weekDayLetterPaint = Paint(textPaint)
            if (i == currDayOfWeek && !isPrintVersion) {
                weekDayLetterPaint = Paint(getColoredPaint(primaryColor))
            } else if (highlightWeekends && isWeekend(i, config.isSundayFirst)) {
                weekDayLetterPaint = getColoredPaint(redTextColor)
            }
            weekDayLetterPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(dayLetters[i], xPos, weekDaysLetterHeight * 0.7f, weekDayLetterPaint)
        }
    }

    private fun addWeekNumbers(canvas: Canvas) {
        val weekNumberPaint = Paint(textPaint)
        weekNumberPaint.textAlign = Paint.Align.RIGHT

        for (i in 0 until ROW_COUNT) {
            val weekDays = days.subList(i * 7, i * 7 + 7)
            weekNumberPaint.color = if (weekDays.any { it.isToday && !isPrintVersion }) primaryColor else textColor

            // fourth day of the week determines the week of the year number
            val weekOfYear = days.getOrNull(i * 7 + 3)?.weekOfYear ?: 1
            val id = "$weekOfYear:"
            val yPos = i * dayHeight + weekDaysLetterHeight
            canvas.drawText(id, horizontalOffset.toFloat() * 0.9f, yPos + textPaint.textSize, weekNumberPaint)
        }
    }

    private fun measureDaySize(canvas: Canvas) {
        dayWidth = (canvas.width - horizontalOffset) / 7f
        dayHeight = (canvas.height - weekDaysLetterHeight) / ROW_COUNT.toFloat()
        val availableHeightForEvents = dayHeight.toInt() - weekDaysLetterHeight
        maxEventsPerDay = availableHeightForEvents / eventTitleHeight
    }

    private fun drawEvent(event: MonthViewEvent, canvas: Canvas) {
        var verticalOffset = 0
        for (i in 0 until Math.min(event.daysCnt, 7 - event.startDayIndex % 7)) {
            verticalOffset = Math.max(verticalOffset, dayVerticalOffsets[event.startDayIndex + i])
        }
        val xPos = event.startDayIndex % 7 * dayWidth + horizontalOffset
        val yPos = (event.startDayIndex / 7) * dayHeight + dayVerticalOffset
        val xPosCenter = xPos + dayWidth / 2

        if (verticalOffset - eventTitleHeight * 2 > dayHeight) {
            canvas.drawText("...", xPosCenter, yPos + verticalOffset - eventTitleHeight / 2, getTextPaint(days[event.startDayIndex]))
            return
        }

        // event background rectangle
        val backgroundY = yPos + verticalOffset
        val bgLeft = xPos + smallPadding
        val bgTop = backgroundY + smallPadding - eventTitleHeight
        var bgRight = xPos - smallPadding + dayWidth * event.daysCnt
        val bgBottom = backgroundY + smallPadding * 2
        if (bgRight > canvas.width.toFloat()) {
            bgRight = canvas.width.toFloat() - smallPadding
            val newStartDayIndex = (event.startDayIndex / 7 + 1) * 7
            if (newStartDayIndex < 42) {
                val newEvent = event.copy(startDayIndex = newStartDayIndex, daysCnt = event.daysCnt - (newStartDayIndex - event.startDayIndex))
                drawEvent(newEvent, canvas)
            }
        }

        val startDayIndex = days[event.originalStartDayIndex]
        val endDayIndex = days[Math.min(event.startDayIndex + event.daysCnt - 1, 41)]
        bgRectF.set(bgLeft, bgTop, bgRight, bgBottom)
        canvas.drawRoundRect(bgRectF, BG_CORNER_RADIUS, BG_CORNER_RADIUS, getEventBackgroundColor(event, startDayIndex, endDayIndex))

        drawEventTitle(event, canvas, xPos, yPos + verticalOffset, bgRight - bgLeft - smallPadding)

        for (i in 0 until Math.min(event.daysCnt, 7 - event.startDayIndex % 7)) {
            dayVerticalOffsets.put(event.startDayIndex + i, verticalOffset + eventTitleHeight + smallPadding * 2)
        }
    }

    private fun drawEventTitle(event: MonthViewEvent, canvas: Canvas, x: Float, y: Float, availableWidth: Float) {
        val ellipsized = TextUtils.ellipsize(event.title, eventTitlePaint, availableWidth - smallPadding, TextUtils.TruncateAt.END)
        canvas.drawText(event.title, 0, ellipsized.length, x + smallPadding * 2, y, getEventTitlePaint(event))
    }

    private fun getTextPaint(startDay: DayMonthly): Paint {
        var paintColor = textColor
        if (!isPrintVersion) {
            if (startDay.isToday) {
                paintColor = primaryColor.getContrastColor()
            } else if (highlightWeekends && startDay.isWeekend) {
                paintColor = redTextColor
            }
        }

        if (!startDay.isThisMonth) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getLunarTextPaint(startDay: DayMonthly): Paint {
        var paintColor = textColor
        if (!startDay.isThisMonth) {
            paintColor = paintColor.adjustAlpha(LOW_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getColoredPaint(color: Int): Paint {
        val curPaint = Paint(textPaint)
        curPaint.color = color
        return curPaint
    }

    private fun getEventBackgroundColor(event: MonthViewEvent, startDay: DayMonthly, endDay: DayMonthly): Paint {
        var paintColor = event.color
        if ((!startDay.isThisMonth && !endDay.isThisMonth) || (dimPastEvents && event.isPastEvent && !isPrintVersion)) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getEventTitlePaint(event: MonthViewEvent): Paint {
        val curPaint = Paint(eventTitlePaint)
        curPaint.color = event.color.getContrastColor()
        return curPaint
    }

    private fun getCirclePaint(day: DayMonthly): Paint {
        val curPaint = Paint(textPaint)
        var paintColor = primaryColor
        if (!day.isThisMonth) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }
        curPaint.color = paintColor
        return curPaint
    }

    private fun initWeekDayLetters() {
        dayLetters = context.resources.getStringArray(R.array.week_day_letters).toMutableList() as ArrayList<String>
        if (config.isSundayFirst) {
            dayLetters.moveLastItemToFront()
        }
    }

    private fun setupCurrentDayOfWeekIndex() {
        if (days.firstOrNull { it.isToday && it.isThisMonth } == null) {
            currDayOfWeek = -1
            return
        }

        currDayOfWeek = DateTime().dayOfWeek
        if (config.isSundayFirst) {
            currDayOfWeek %= 7
        } else {
            currDayOfWeek--
        }
    }

    // take into account cases when an event starts on the previous screen, subtract those days
    private fun getEventLastingDaysCount(event: Event): Int {
        val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
        val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
        val code = days.first().code
        val screenStartDateTime = Formatter.getDateTimeFromCode(code).toLocalDate()
        var eventStartDateTime = Formatter.getDateTimeFromTS(startDateTime.seconds()).toLocalDate()
        val eventEndDateTime = Formatter.getDateTimeFromTS(endDateTime.seconds()).toLocalDate()
        val diff = Days.daysBetween(screenStartDateTime, eventStartDateTime).days
        if (diff < 0) {
            eventStartDateTime = screenStartDateTime
        }

        val isMidnight = Formatter.getDateTimeFromTS(endDateTime.seconds()) == Formatter.getDateTimeFromTS(endDateTime.seconds()).withTimeAtStartOfDay()
        val numDays = Days.daysBetween(eventStartDateTime, eventEndDateTime).days
        val daysCnt = if (numDays == 1 && isMidnight) 0 else numDays
        return daysCnt + 1
    }

    private fun isDayValid(event: Event, code: String): Boolean {
        val date = Formatter.getDateTimeFromCode(code)
        return event.startTS != event.endTS && Formatter.getDateTimeFromTS(event.endTS) == Formatter.getDateTimeFromTS(date.seconds()).withTimeAtStartOfDay()
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.theme_light_text_color)
        } else {
            config.textColor
        }

        textPaint.color = textColor
        gridPaint.color = textColor.adjustAlpha(LOW_ALPHA)
        invalidate()
        initWeekDayLetters()
    }
}
