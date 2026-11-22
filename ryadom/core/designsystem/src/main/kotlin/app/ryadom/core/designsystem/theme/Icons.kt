package app.ryadom.core.designsystem.theme

import androidx.annotation.DrawableRes
import app.ryadom.core.designsystem.R

/** Иконки «Рядом»: собственный набор, сетка 24, линия 1.75. Векторы генерирует design/tools/build.py. */
object RyIcons {
    @DrawableRes val Home = R.drawable.ic_home
    @DrawableRes val Calendar = R.drawable.ic_calendar
    @DrawableRes val Family = R.drawable.ic_family
    @DrawableRes val History = R.drawable.ic_history
    @DrawableRes val Plus = R.drawable.ic_plus
    @DrawableRes val Check = R.drawable.ic_check
    @DrawableRes val CheckCircle = R.drawable.ic_check_circle
    @DrawableRes val Later = R.drawable.ic_later
    @DrawableRes val Skip = R.drawable.ic_skip
    @DrawableRes val Close = R.drawable.ic_close
    @DrawableRes val More = R.drawable.ic_more
    @DrawableRes val Edit = R.drawable.ic_edit
    @DrawableRes val Trash = R.drawable.ic_trash
    @DrawableRes val Archive = R.drawable.ic_archive
    @DrawableRes val Search = R.drawable.ic_search
    @DrawableRes val Send = R.drawable.ic_send
    @DrawableRes val Share = R.drawable.ic_share
    @DrawableRes val Link = R.drawable.ic_link
    @DrawableRes val Camera = R.drawable.ic_camera
    @DrawableRes val Download = R.drawable.ic_download
    @DrawableRes val Logout = R.drawable.ic_logout
    @DrawableRes val ChevronRight = R.drawable.ic_chevron_right
    @DrawableRes val ChevronLeft = R.drawable.ic_chevron_left
    @DrawableRes val ChevronDown = R.drawable.ic_chevron_down
    @DrawableRes val ArrowLeft = R.drawable.ic_arrow_left
    @DrawableRes val Capsule = R.drawable.ic_capsule
    @DrawableRes val Tablet = R.drawable.ic_tablet
    @DrawableRes val Drop = R.drawable.ic_drop
    @DrawableRes val Measure = R.drawable.ic_measure
    @DrawableRes val Sachet = R.drawable.ic_sachet
    @DrawableRes val Jar = R.drawable.ic_jar
    @DrawableRes val Glass = R.drawable.ic_glass
    @DrawableRes val Box = R.drawable.ic_box
    @DrawableRes val Bag = R.drawable.ic_bag
    @DrawableRes val FirstAid = R.drawable.ic_first_aid
    @DrawableRes val Note = R.drawable.ic_note
    @DrawableRes val Flag = R.drawable.ic_flag
    @DrawableRes val Sunrise = R.drawable.ic_sunrise
    @DrawableRes val Sun = R.drawable.ic_sun
    @DrawableRes val Moon = R.drawable.ic_moon
    @DrawableRes val Clock = R.drawable.ic_clock
    @DrawableRes val Repeat = R.drawable.ic_repeat
    @DrawableRes val User = R.drawable.ic_user
    @DrawableRes val UserPlus = R.drawable.ic_user_plus
    @DrawableRes val Child = R.drawable.ic_child
    @DrawableRes val Heart = R.drawable.ic_heart
    @DrawableRes val Message = R.drawable.ic_message
    @DrawableRes val Shield = R.drawable.ic_shield
    @DrawableRes val Bell = R.drawable.ic_bell
    @DrawableRes val BellOff = R.drawable.ic_bell_off
    @DrawableRes val Settings = R.drawable.ic_settings
    @DrawableRes val Sync = R.drawable.ic_sync
    @DrawableRes val CloudCheck = R.drawable.ic_cloud_check
    @DrawableRes val CloudOff = R.drawable.ic_cloud_off
    @DrawableRes val Info = R.drawable.ic_info
    @DrawableRes val Lock = R.drawable.ic_lock
    @DrawableRes val Globe = R.drawable.ic_globe
    @DrawableRes val Theme = R.drawable.ic_theme
    @DrawableRes val Volume = R.drawable.ic_volume
    @DrawableRes val Vibrate = R.drawable.ic_vibrate
    @DrawableRes val Chart = R.drawable.ic_chart
    @DrawableRes val Pause = R.drawable.ic_pause
    @DrawableRes val Leaf = R.drawable.ic_leaf
    @DrawableRes val Logo = R.drawable.logo_mark
    @DrawableRes val NotificationSmall = R.drawable.ic_stat_ryadom
}

/** Иллюстрации: 20 сцен одной коллекции, светлая и тёмная версии подставляются системой (drawable-night). */
enum class Illustration(@DrawableRes val res: Int) {
    Symbol(R.drawable.il_symbol),
    Vitamins(R.drawable.il_vitamins),
    Water(R.drawable.il_water),
    FamilyCare(R.drawable.il_family_care),
    MotherChild(R.drawable.il_mother_child),
    FatherChild(R.drawable.il_father_child),
    FamilyScene(R.drawable.il_family_scene),
    Morning(R.drawable.il_morning),
    Evening(R.drawable.il_evening),
    Package(R.drawable.il_package),
    FirstAid(R.drawable.il_first_aid),
    Reminder(R.drawable.il_reminder),
    Calendar(R.drawable.il_calendar),
    IntakeDone(R.drawable.il_intake_done),
    EmptySchedule(R.drawable.il_empty_schedule),
    Missed(R.drawable.il_missed),
    CourseComplete(R.drawable.il_course_complete),
    LowStock(R.drawable.il_low_stock),
    History(R.drawable.il_history),
    DayComplete(R.drawable.il_day_complete),
}
