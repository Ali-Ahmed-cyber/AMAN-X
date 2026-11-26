package com.amanx.app.data.academy

import com.amanx.app.domain.model.AcademyLesson
import com.amanx.app.domain.model.FraudSimulationChoice
import com.amanx.app.domain.model.FraudSimulationScenario
import com.amanx.app.domain.model.FraudSimulationStep
import com.amanx.app.domain.model.LocalizedText

object AcademyContentProvider {

    val lessons: List<AcademyLesson> = listOf(
        AcademyLesson(
            id = "lesson_otp_shield",
            title = LocalizedText(
                ar = "لماذا لا يطلب البنك رمز التحقق (OTP) أبداً؟",
                en = "Why Banks Never Ask for Your OTP Code"
            ),
            summary = LocalizedText(
                ar = "فهم دور رمز التحقق المؤقت وكيف يستغل المحتالون الاستعجال لسرقته.",
                en = "Understand one-time passcodes and how scammers use urgency to steal them."
            ),
            body = LocalizedText(
                ar = "رمز التحقق (OTP) هو مفتاح تأكيد شخصي صُمم لك وحدك لإتمام عملية أو تسجيل دخول. أي شخص يتصل بك أو يراسلك مدعياً أنه من خدمة العملاء ويطلب قراءة هذا الرمز لإلغاء خصم أو تحديث حسابك يحاول فعلياً الدخول لحسابك أو تأكيد تحويل لصالحه.",
                en = "A One-Time Password (OTP) is your personal authorization key to complete a login or transfer. Anyone calling or messaging claiming to be customer support and asking for this code to cancel a charge or update your account is attempting unauthorized access."
            ),
            keyTakeaways = listOf(
                LocalizedText("لا تشارك رمز OTP أو الرقم السري مع أي موظف أو قريب.", "Never share an OTP or PIN with any staff member or caller."),
                LocalizedText("أنهِ المكالمة فوراً وتواصل فقط عبر الرقم الرسمي على بطاقتك.", "Hang up immediately and call only the official number on your card.")
            ),
            quizQuestion = LocalizedText(
                ar = "اتصل بك شخص باسم دعم البنك وطلب رمز التحقق لإيقاف عملية سحب مشبوهة، ما التصرف الصحيح؟",
                en = "Someone claiming to be bank support asks for your OTP to stop a suspicious withdrawal. What should you do?"
            ),
            quizOptions = listOf(
                LocalizedText("إعطاؤه الرمز بسرعة لإيقاف السحب", "Give them the code quickly to stop the withdrawal"),
                LocalizedText("رفض مشاركة الرمز، إنهاء الاتصال، والاتصال بالرقم الرسمي للبنك", "Refuse to share the code, hang up, and call the bank's official number"),
                LocalizedText("إرسال الرمز في رسالة واتساب", "Send the code over WhatsApp")
            ),
            correctOptionIndex = 1
        ),
        AcademyLesson(
            id = "lesson_impersonation",
            title = LocalizedText(
                ar = "كشف انتحال صفة موظفي البنوك والمحافظ",
                en = "Spotting Bank & Wallet Impersonation"
            ),
            summary = LocalizedText(
                ar = "علامات انتحال الهوية في المكالمات والرسائل النصية وتطبيقات المحادثة.",
                en = "Key signs of impersonation across calls, messages, and chat apps."
            ),
            body = LocalizedText(
                ar = "يستخدم المنتحلون لهجة رسمية أو شعارات مقتبسة، وغالباً ما يعتمدون على تخويفك بتجميد الحساب أو إغرائك بجائزة. الجهات المصرفية الموثوقة لا تستخدم أرقاماً شخصية في واتساب لحل مشكلات الحسابات ولا تطلب تحويل رسوم لتفعيل محفظتك.",
                en = "Impersonators copy official logos and formal tone, often pressuring you with account suspension or fake rewards. Legitimate financial institutions do not use personal chat numbers to unlock accounts or charge activation fees."
            ),
            keyTakeaways = listOf(
                LocalizedText("تحقق دائماً من القناة الرسمية لا من الرقم الذي تواصل معك.", "Always verify through an official channel, never the inbound contact."),
                LocalizedText("الاستعجال الشديد علامة خطر كلاسيكية في الهندسة الاجتماعية.", "Artificial urgency is a classic social engineering red flag.")
            ),
            quizQuestion = LocalizedText(
                ar = "وصلتك رسالة من رقم شخصي بشعار محفظة مالية تطلب رسوماً لتحديث حسابك قبل الإغلاق، ما التقييم الأصح؟",
                en = "A personal number using a wallet logo demands a fee to update your account before closure. What does this indicate?"
            ),
            quizOptions = listOf(
                LocalizedText("إجراء روتيني آمن", "A safe routine procedure"),
                LocalizedText("مؤشر قوي على انتحال صفة ومحاولة احتيال", "A strong indicator of impersonation and fraud"),
                LocalizedText("يجب الدفع أولاً ثم السؤال", "Pay first and ask later")
            ),
            correctOptionIndex = 1
        ),
        AcademyLesson(
            id = "lesson_mistaken_transfer",
            title = LocalizedText(
                ar = "خدعة التحويل بالخطأ وإعادة الإرسال",
                en = "The Mistaken-Transfer Scam"
            ),
            summary = LocalizedText(
                ar = "كيف يتحقق المحتال من خداعك برسالة إشعار مزيفة أو تحويل مؤقت.",
                en = "How scammers fabricate transfer alerts to trick you into sending real money."
            ),
            body = LocalizedText(
                ar = "قد يصلك نص أو رسالة تدّعي أنه تم تحويل مبلغ لحسابك بالخطأ مع طلب إعادته فوراً لرقم آخر. لا تعتمد أبداً على لقطة شاشة أو رسالة نصية مرسلة إليك؛ افتح تطبيقك البنكي الرسمي بنفسك وتأكد من الرصيد الفعلي وتفاصيل العملية قبل أي خطوة.",
                en = "You may receive a message claiming funds were sent to you by mistake and asking you to send them back immediately to another account. Never trust forwarded screenshots or SMS claims—always check your official banking app balance directly."
            ),
            keyTakeaways = listOf(
                LocalizedText("إشعارات الرسائل النصية ليست دليلاً مالياً نهائياً.", "Text notifications alone are not proof of settled funds."),
                LocalizedText("راجع رصيدك الفعلي من داخل تطبيقك البنكي الرسمي.", "Verify your actual settled balance inside your official banking app.")
            ),
            quizQuestion = LocalizedText(
                ar = "طلب منك متصل إعادة مبلغ يقول إنه حوله بالخطأ بناءً على رسالة نصية وصلتك، ماذا تفعل أولاً؟",
                en = "A caller asks you to refund a mistaken transfer based on a text you received. What should you do first?"
            ),
            quizOptions = listOf(
                LocalizedText("فتح التطبيق البنكي الرسمي والتحقق من الرصيد الفعلي والعمليات", "Open your official banking app to verify the actual balance and transactions"),
                LocalizedText("التحويل فوراً لعدم إحراج المتصل", "Transfer immediately to avoid embarrassment"),
                LocalizedText("مشاركة رقم حسابك وكلمة المرور ليتأكد", "Share your account password so they can check")
            ),
            correctOptionIndex = 0
        ),
        AcademyLesson(
            id = "lesson_evidence_preservation",
            title = LocalizedText(
                ar = "كيف تحفظ الأدلة بشكل صحيح عند الاشتباه باحتيال؟",
                en = "How to Preserve Evidence Properly"
            ),
            summary = LocalizedText(
                ar = "خطوات عملية لحفظ التفاصيل دون مسحها قبل التواصل مع البنك أو الجهات المختصة.",
                en = "Practical steps to preserve details safely before contacting your bank or authorities."
            ),
            body = LocalizedText(
                ar = "عند الاشتباه في محاولة احتيال، تجنب حذف المحادثة أو مسح الإشعارات قبل توثيقها. احفظ النص الأصلي والتوقيت واسم المصدر في خزنة الأدلة المشفرة داخل AMAN X، وتذكر أن التطبيق لا يوقف التحويلات البنكية لكنه يرتب خطواتك وأدلتك بسرعة.",
                en = "When suspecting fraud, avoid deleting chats or dismissing notifications before documenting them. Save the text, timestamp, and source inside AMAN X's encrypted Evidence Vault. Remember AMAN X cannot stop bank transfers, so contact your bank directly as well."
            ),
            keyTakeaways = listOf(
                LocalizedText("التوثيق الفوري يحافظ على التسلسل الزمني للحادث.", "Immediate documentation preserves an accurate incident timeline."),
                LocalizedText("تواصل مع بنكك فوراً إذا تم تحويل مالي فعلي.", "Contact your bank immediately if an actual transfer occurred.")
            ),
            quizQuestion = LocalizedText(
                ar = "ما الدور الفعلي لمركز الطوارئ وخزنة الأدلة في AMAN X؟",
                en = "What is the real role of AMAN X Emergency Center and Evidence Vault?"
            ),
            quizOptions = listOf(
                LocalizedText("إيقاف التحويلات البنكية عن بعد تلقائياً", "Automatically stopping bank transfers remotely"),
                LocalizedText("حفظ الأدلة محلياً بتشفير وإرشادك لخطوات التصرف الآمن", "Encrypting evidence locally and guiding safe response steps"),
                LocalizedText("الدخول لحسابك البنكي بالنيابة عنك", "Logging into your bank account on your behalf")
            ),
            correctOptionIndex = 1
        )
    )

    val simulations: List<FraudSimulationScenario> = listOf(
        FraudSimulationScenario(
            id = "sim_support_otp",
            title = LocalizedText(
                ar = "محاكاة: مكالمة دعم فني عاجلة",
                en = "Simulation: Urgent Bank Support Call"
            ),
            contextLabel = LocalizedText(
                ar = "محاكاة تعليمية معزولة — لا تتصل بأي بنك حقيقي",
                en = "Isolated educational simulation — no real banking connection"
            ),
            redFlags = listOf(
                LocalizedText("ضغط زمني وتهديد بتجميد الحساب", "Time pressure and threat of account freeze"),
                LocalizedText("طلب قراءة رمز تحقق وصل لهاتفك", "Asking you to read back an OTP sent to your phone")
            ),
            steps = listOf(
                FraudSimulationStep(
                    prompt = LocalizedText(
                        ar = "يصلك اتصال: «مرحباً، معك قسم الأمان في البنك. رصدنا محاولة سحب الآن، أرسل لي رمز التحقق المكون من 6 أرقام لإلغائها خلال 30 ثانية».",
                        en = "A caller says: 'Hello, Bank Security Department. We detected a withdrawal attempt; read the 6-digit OTP to cancel it within 30 seconds.'"
                    ),
                    choices = listOf(
                        FraudSimulationChoice(
                            label = LocalizedText("أقرأ له الرمز بسرعة لحماية حسابي", "Read the code quickly to protect my account"),
                            isSafeChoice = false,
                            feedback = LocalizedText(
                                ar = "خطر! الموظف الحقيقي لا يطلب رمز OTP أبداً؛ إعطاؤه الرمز يمكّنه من تنفيذ العملية فعلياً.",
                                en = "Risky! Legitimate staff never ask for an OTP. Sharing it lets the attacker authorize the action."
                            )
                        ),
                        FraudSimulationChoice(
                            label = LocalizedText("أرفض إعطاء الرمز وأغلق المكالمة وأتصل برقم البنك الرسمي", "Refuse to share the code, hang up, and call the bank's official number"),
                            isSafeChoice = true,
                            feedback = LocalizedText(
                                ar = "تصرف مثالي! قطعت محاولة الهندسة الاجتماعية وحافظت على سرية رمز التحقق.",
                                en = "Great response! You broke the social engineering attempt and protected your OTP."
                            )
                        )
                    )
                ),
                FraudSimulationStep(
                    prompt = LocalizedText(
                        ar = "بعد إنهاء المكالمة، أرسل لك نفس الرقم رسالة تهديد بأن حسابك سيتوقف اليوم. ماذا تفعل؟",
                        en = "After hanging up, the same number sends a threatening message that your account will be closed today. What next?"
                    ),
                    choices = listOf(
                        FraudSimulationChoice(
                            label = LocalizedText("أفحص الرسالة وأحفظها كدليل في AMAN X ولا أتفاعل مع المرسل", "Analyze and save the message as evidence in AMAN X without replying"),
                            isSafeChoice = true,
                            feedback = LocalizedText(
                                ar = "ممتاز! حفظ الدليل وتجاهل المرسل يمنع التصعيد ويوثق المحاولة.",
                                en = "Excellent! Preserving evidence while ignoring the sender prevents escalation."
                            )
                        ),
                        FraudSimulationChoice(
                            label = LocalizedText("أرد عليهم وأرسل صورة بطاقتي لإثبات هويتي", "Reply and send a photo of my card to prove my identity"),
                            isSafeChoice = false,
                            feedback = LocalizedText(
                                ar = "غير آمن! مشاركة بيانات البطاقة مع رقم غير موثوق يعرضك للاحتيال المباشر.",
                                en = "Unsafe! Sharing card details with an unverified contact exposes you to direct fraud."
                            )
                        )
                    )
                )
            )
        ),
        FraudSimulationScenario(
            id = "sim_fake_refund",
            title = LocalizedText(
                ar = "محاكاة: حوالة بالخطأ عبر محفظة",
                en = "Simulation: Mistaken Wallet Transfer"
            ),
            contextLabel = LocalizedText(
                ar = "محاكاة تعليمية معزولة — لا تتصل بأي محفظة حقيقية",
                en = "Isolated educational simulation — no real wallet connection"
            ),
            redFlags = listOf(
                LocalizedText("طلب تحويل عاجل إلى رقم مختلف عن المرسل", "Urgent request to send money to a different number"),
                LocalizedText("الاعتماد على رسالة نصية بدل التحقق داخل التطبيق الرسمي", "Relying on a text message instead of checking your official app")
            ),
            steps = listOf(
                FraudSimulationStep(
                    prompt = LocalizedText(
                        ar = "تصلك رسالة نصية من رقم عادي بصيغة مشابهة لإشعار إيداع، ثم يتصل شخص ويقول: «أرسلت لك مبلغاً بالخطأ، حوله فوراً لهذا الرقم الآخر لأني مسافر».",
                        en = "You receive a normal SMS styled like a deposit alert, followed by a call: 'I sent you money by mistake; please transfer it urgently to this other number.'"
                    ),
                    choices = listOf(
                        FraudSimulationChoice(
                            label = LocalizedText("أفتح تطبيق المحفظة الرسمي أولاً وأتحقق من سجل العمليات والرصيد الفعلي", "Open my official wallet app first to check actual balance and history"),
                            isSafeChoice = true,
                            feedback = LocalizedText(
                                ar = "صحيح! التحقق من التطبيق الرسمي يكشف فوراً إن كانت الرسالة مفبركة أم حقيقية.",
                                en = "Correct! Checking the official app immediately reveals whether any real funds arrived."
                            )
                        ),
                        FraudSimulationChoice(
                            label = LocalizedText("أحوّل المبلغ للرقم الجديد فوراً اعتماداً على الرسالة النصية", "Transfer immediately to the new number based on the SMS"),
                            isSafeChoice = false,
                            feedback = LocalizedText(
                                ar = "فخ شائع! الرسائل النصية يمكن تزويرها بسهولة لتبدو كإشعارات بنكية.",
                                en = "Common trap! SMS messages can be crafted to look like deposit alerts."
                            )
                        )
                    )
                )
            )
        )
    )
}
