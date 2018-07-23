package com.jonaswanke.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import java.util.*
import kotlin.math.max
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class DayView @JvmOverloads constructor(context: Context,
                                        attrs: AttributeSet? = null,
                                        @AttrRes defStyleAttr: Int = R.attr.dayViewStyle)
    : ViewGroup(context, attrs, defStyleAttr) {

    var onEventClickListener: ((String) -> Unit)? = null
    var onEventLongClickListener: ((String) -> Unit)? = null

    var day: Day by Delegates.observable(Day()) { _, old, new ->
        if (old == new)
            return@observable

        events = emptyList()
    }
    val start: Long
        get() = day.toCalendar().timeInMillis
    val end: Long
        get() = day.toCalendar().timeInMillis + DateUtils.DAY_IN_MILLIS
    var events: List<Event> by Delegates.observable(emptyList()) { _, old, new ->
        if (old == new)
            return@observable
        if (new.any { event -> event.start < start || event.start >= end })
            throw IllegalArgumentException("event starts must all be inside the set day")

        removeAllViews()
        for (event in new)
            addView(EventView(context).apply {
                this.event = event
            })
        invalidate()
    }

    private val dateSize: Int
    private val dateColor: Int
    private val datePaint: TextPaint
    private val currentDateColor: Int
    private val currentDatePaint: TextPaint
    private val weekDaySize: Int
    private val weekDayColor: Int
    private val weekDayPaint: TextPaint
    private val currentWeekDayColor: Int
    private val currentWeekDayPaint: TextPaint

    private var divider by Delegates.observable<Drawable?>(null) { _, _, new ->
        dividerHeight = new?.intrinsicWidth ?: 0
    }
    private var dividerHeight: Int = 0

    init {
        setWillNotDraw(false)
        divider = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright)

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.DayView, defStyleAttr, R.style.Calendar_DayViewStyle)

        dateSize = a.getDimensionPixelSize(R.styleable.DayView_dateSize, 16)
        dateColor = a.getColor(R.styleable.DayView_dateColor,
                ContextCompat.getColor(context, android.R.color.secondary_text_light))
        datePaint = TextPaint().apply {
            color = dateColor
            isAntiAlias = true
            textSize = dateSize.toFloat()
        }
        currentDateColor = a.getColor(R.styleable.DayView_currentDateColor, dateColor)
        currentDatePaint = TextPaint().apply {
            color = currentDateColor
            isAntiAlias = true
            textSize = dateSize.toFloat()
        }

        weekDaySize = a.getDimensionPixelSize(R.styleable.DayView_weekDaySize, 16)
        weekDayColor = a.getColor(R.styleable.DayView_weekDayColor,
                ContextCompat.getColor(context, android.R.color.secondary_text_light))
        weekDayPaint = TextPaint().apply {
            color = weekDayColor
            isAntiAlias = true
            textSize = weekDaySize.toFloat()
        }
        currentWeekDayColor = a.getColor(R.styleable.DayView_currentWeekDayColor, weekDayColor)
        currentWeekDayPaint = TextPaint().apply {
            color = currentWeekDayColor
            isAntiAlias = true
            textSize = weekDaySize.toFloat()
        }

        a.recycle()
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child !is EventView)
            throw IllegalArgumentException("Only EventViews may be children of DayView")
        super.addView(child, index, params)
    }

    private val locale: Locale = Locale.getDefault()
    private val cal = Calendar.getInstance()
    private val headerHeight = (dateSize * 1.75 + weekDaySize * 2).toInt()
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val left = paddingLeft
        val top = paddingTop + headerHeight
        val right = r - l - paddingRight
        val bottom = b - t - paddingBottom
        val height = bottom - top

        fun getPosForTime(time: Long): Int {
            return (height * cal.apply { timeInMillis = time }.timeOfDay / DateUtils.DAY_IN_MILLIS).toInt()
        }

        for (viewIndex in 0 until childCount) {
            val view = getChildAt(viewIndex) as EventView
            val event = view.event ?: continue
            val startHeight = top + getPosForTime(event.start)
            val endHeight = max(top + getPosForTime(event.end), startHeight + view.minHeight)

            view.layout(left, startHeight, right, endHeight)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val left = paddingLeft
        var top = paddingTop
        val right = canvas.width - paddingRight
        val bottom = canvas.height - paddingBottom

        val isToday = DateUtils.isToday(start)
        cal.timeInMillis = start

        top += (dateSize * 1.5).toInt()
        canvas.drawText(cal.get(Calendar.DAY_OF_MONTH).toString(),
                .3f * dateSize, top.toFloat(), if (isToday) currentDatePaint else datePaint)
        top += (dateSize * .25).toInt()

        top += weekDaySize
        canvas.drawText(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale),
                .3f * dateSize, top.toFloat(), if (isToday) currentWeekDayPaint else weekDayPaint)
        top += weekDaySize

        val hourHeight = (bottom - top) / 23
        for (hour in 0..24) {
            divider?.setBounds(left, top + hourHeight * hour,
                    right, top + hourHeight * hour + dividerHeight)
            divider?.draw(canvas)
        }
    }
}
