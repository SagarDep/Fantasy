package com.wosloveslife.fantasy.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.makeramen.roundedimageview.RoundedImageView;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.orhanobut.logger.Logger;
import com.wosloveslife.fantasy.R;
import com.wosloveslife.fantasy.adapter.ExoPlayerEventListenerAdapter;
import com.wosloveslife.fantasy.adapter.SubscriberAdapter;
import com.wosloveslife.fantasy.bean.BMusic;
import com.wosloveslife.fantasy.utils.FormatUtils;
import com.yesing.blibrary_wos.utils.photo.BitmapUtils;
import com.yesing.blibrary_wos.utils.screenAdaptation.Dp2Px;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import stackblur_java.StackBlurManager;

import static android.support.v7.graphics.Palette.from;

/**
 * Created by zhangh on 2017/1/15.
 */

public class ControlView extends FrameLayout implements NestedScrollingParent {
    private static final float PROGRESS_MAX = 100;

    @BindView(R.id.fl_root)
    FrameLayout mFlRoot;
    @BindView(R.id.iv_bg)
    ImageView mIvBg;
    /**
     * 这个控件是专门用来覆盖在mIvBg上面用来在切换头部背景时的CircularReveal动画使用,
     * 这样可以在原有背景色基础上有一个新的背景色展开的效果
     * 应该有更好的实现方式,暂时用这个
     */
    @BindView(R.id.iv_bg_scrim)
    ImageView mIvBgScrim;
    @BindView(R.id.iv_album)
    RoundedImageView mIvAlbum;
    /** 歌曲名 */
    @BindView(R.id.tv_title)
    TextView mTvTitle;
    /** 艺术家 */
    @BindView(R.id.tv_artist)
    TextView mTvArtist;
    /** 当前进度 */
    @BindView(R.id.tv_progress)
    TextView mTvProgress;
    /** 总进度 */
    @BindView(R.id.tv_duration)
    TextView mTvDuration;

    /** 播放/暂停 */
    @BindView(R.id.iv_play_btn)
    ImageView mIvPlayBtn;
    /** 上一曲按钮 */
    @BindView(R.id.iv_previous_btn)
    ImageView mIvPreviousBtn;
    /** 下一曲按钮 */
    @BindView(R.id.iv_next_btn)
    ImageView mIvNextBtn;

    /** 进度条(不可拖动) */
    @BindView(R.id.pb_progress)
    ProgressBar mPbProgress;
    /** 二段展开时的播放/暂停按钮 */
    @BindView(R.id.fac_play_btn)
    FloatingActionButton mFacPlayBtn;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    //==============
    private BMusic mCurrentMusic;
    private boolean mIsOnline;

    //=============
    private SimpleExoPlayer mPlayer;

    //=============Var
    /** 如果手正在拖动SeekBar,就不能让Progress自动跳转 */
    boolean mDragging;

    //=============联动相关
    NestedScrollingParentHelper mParentHelper;
    View mNestedScrollingChild;
    //=======
    /** 展开时的高度 */
    int mHeadMaxHeight;
    /** 收起时的高度 */
    int mHeadMinHeight;
    /** 从收起到展开总共偏移的距离(即 mHeadMaxHeight - mHeadMinHeight ) */
    int mMaxOffsetY;
    /** 用来记录当前头部布局的高度,因为mFlRoot.getHeight()获得到的高度可能正在设置中,会和真实的高度有偏差 */
    int mCurrentHeight;
    //=======
    /** 最后一次设置控件高度时是展开的还是收起的.true=展开中,false=收起中 */
    boolean mExpanding;
    /** 记录当前控件的展开形态 */
    boolean mIsExpanded;
    /** 记录当前控件的显示模式: true普通Toolbar,false歌曲控制 */
    private boolean mIsToolbarShown;
    //======
    /** 用于计算展开/收起的动画 */
    ValueAnimator mAnimator;
    //======
    VelocityTracker mVelocityTracker;
    int mTouchSlop;
    int mMinimumFlingVelocity;
    int mMaximumFlingVelocity;
    int mScrollPointerId;
    //======
    /** 封面的最大弧度(为圆形时) */
    int mAlbumMaxRadius;
    /** 歌曲名/艺术家/播放进度文字等的最小左边距,同时也是最大向左偏移量 */
    int mMinLeftMargin;
    int mAlbumSize;
    /** 播放总时长文字的最大向右偏移量 */
    int mDurationRightMargin;
    int mStatusBarHeight;

    //============
    private Drawable mDefAlbum;
    private Drawable mDefBlurredAlbum;
    private Drawable mDefColorMutedBg;
    private Drawable mDefColorTitle;

    private Drawable mAlbum;
    /** 在当前封面的基础上进行了模糊处理,作为展开时的背景图片 */
    private Drawable mBlurredAlbum;
    private Drawable mColorMutedBg;
    private Drawable mColorTitle;

    private ID3v2 mCurrentId3v2;

    public ControlView(Context context) {
        this(context, null);
    }

    public ControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ControlView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mHeadMinHeight = Dp2Px.toPX(getContext(), 56);
        mHeadMaxHeight = Dp2Px.toPX(getContext(), 160);
        mMaxOffsetY = mHeadMaxHeight - mHeadMinHeight;
        /* 圆形的角度等于边长的一半,因为布局中写死了48dp,因此这里取24dp,如果有需要,应该在onSizeChanged()方法中监听子控件的边长除2 */
        mAlbumMaxRadius = Dp2Px.toPX(getContext(), 24);
        mMinLeftMargin = Dp2Px.toPX(getContext(), 56);
        mAlbumSize = Dp2Px.toPX(getContext(), 48);
        mDurationRightMargin = Dp2Px.toPX(getContext(), 58);
        mStatusBarHeight = (int) getResources().getDimension(R.dimen.statusBar_height);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mMinimumFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_control);

        mDefAlbum = new BitmapDrawable(bitmap);
        mDefBlurredAlbum = new BitmapDrawable(new StackBlurManager(bitmap).process(30));
        mDefColorTitle = new ColorDrawable(getResources().getColor(R.color.white));
        mDefColorMutedBg = new ColorDrawable(getResources().getColor(R.color.colorPrimary));

        mVelocityTracker = VelocityTracker.obtain();
        mParentHelper = new NestedScrollingParentHelper(this);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        ControlViewState state = new ControlViewState(super.onSaveInstanceState());
        state.mIsExpanded = mIsExpanded;
        state.mExpanding = mExpanding;
        state.mIsToolbarShown = mIsToolbarShown;
        return state;
    }

    class ControlViewState extends BaseSavedState {
        boolean mExpanding;
        boolean mIsExpanded;
        boolean mIsToolbarShown;

        ControlViewState(Parcelable source) {
            super(source);
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof ControlViewState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        ControlViewState cs = (ControlViewState) state;
        super.onRestoreInstanceState(cs.getSuperState());

        /* 恢复操作 */
        mExpanding = cs.mExpanding;
        mIsExpanded = cs.mIsExpanded;

        if (cs.mIsToolbarShown) {
            toggleToolbarShown(true);
        } else {
            toggleExpand(mIsExpanded);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof NestedScrollingChild) {
                mNestedScrollingChild = childAt;
                childAt.setPadding(0, mHeadMinHeight + mStatusBarHeight, 0, 0);
            }
        }

        ViewGroup.LayoutParams params = mIvBg.getLayoutParams();
        params.height = (int) (mHeadMinHeight + getResources().getDimension(R.dimen.statusBar_height));
        mIvBg.setLayoutParams(params);
        mIvBgScrim.setLayoutParams(params);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        if (mFlRoot != null) return;
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_control, this, false);
        ButterKnife.bind(this, view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPbProgress.setProgressBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.transparent)));
        }

        mFacPlayBtn.hide();

        mFlRoot.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean consume = false;
                float y = event.getY();
                mVelocityTracker.addMovement(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mScrollPointerId = MotionEventCompat.getPointerId(event, 0);
                        mHeadDownY = y;
                        mHeadClick = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(mHeadDownY - y) > mTouchSlop) {
                            int deltaY = Math.round(mHeadLastY - y);
                            setOffsetBy(deltaY);
                            consume = true;
                            mHeadClick = false;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (mHeadClick) {
                            performClick();
                            break;
                        }
                        mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                        float velocityY = VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId);
                        if (Math.abs(velocityY) < mMinimumFlingVelocity
                                || (velocityY < 0 && mFlRoot.getHeight() <= mHeadMinHeight)
                                || (velocityY > 0 && mFlRoot.getHeight() >= mHeadMaxHeight)) {
                            if (mAnimator == null || !mAnimator.isRunning()) {
                                toggleExpand(mCurrentHeight > mHeadMinHeight + mMaxOffsetY / 2);
                            }
                        } else {
                            toggleExpand(velocityY > 0);
                        }

                        mVelocityTracker.clear();
                        consume = true;
                        break;
                }
                mHeadLastY = y;
                return consume;
            }
        });

        addView(view);
    }

    float mHeadLastY;
    float mHeadDownY;
    boolean mHeadClick;

    public void setPlayer(SimpleExoPlayer player) {
        mPlayer = player;

        mPlayer.addListener(new ExoPlayerEventListenerAdapter() {

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                updateProgress();
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
                updateProgress();
            }

            @Override
            public void onPositionDiscontinuity() {
                updateProgress();
            }
        });
    }

    /**
     * 当歌曲切换时通过该功能同步该控件的状态
     *
     * @param music
     */
    public void syncPlayView(BMusic music) {
        if (music == null || mPlayer == null) return;

        if (mPlayer.getPlayWhenReady()) {
            mIvPlayBtn.setImageResource(R.drawable.ic_pause);
            mFacPlayBtn.setImageResource(R.drawable.ic_pause);
        } else {
            mIvPlayBtn.setImageResource(R.drawable.ic_play_arrow);
            mFacPlayBtn.setImageResource(R.drawable.ic_play_arrow);
        }

        if (music.equals(mCurrentMusic)) return;

        mCurrentMusic = music;
        mIsOnline = mCurrentMusic.path.startsWith("http");

        mTvTitle.setText(TextUtils.isEmpty(music.title) ? "未知" : music.title);
        mTvArtist.setText(TextUtils.isEmpty(music.artist) ? "未知" : music.artist);
        mTvProgress.setText("00:00");
        mTvDuration.setText(DateFormat.format("mm:ss", music.duration).toString());

        if (mCurrentMusic == null) return;

        label:
        try {
            Logger.d("开始解析封面 时间 = " + System.currentTimeMillis());
            Mp3File mp3file = new Mp3File(mCurrentMusic.path);
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                if (mCurrentId3v2 != null && TextUtils.equals(mCurrentId3v2.getAlbum(), id3v2Tag.getAlbum())) {
                    break label;
                }
                mCurrentId3v2 = id3v2Tag;


                /* 从歌曲文件的ID3v2字段中读取封面信息并更新封面
                 * 如果没有本地封面,则尝试从网络获取 */
                Observable.create(new Observable.OnSubscribe<Bitmap>() {
                    @Override
                    public void call(Subscriber<? super Bitmap> subscriber) {
                        Bitmap bitmap = null;
                        byte[] image = mCurrentId3v2.getAlbumImage();
                        if (image != null) {
                            Logger.d("从歌曲中读取封面结束 时间 = " + System.currentTimeMillis() + "; 大小 = " + image.length);
                            /* 通过自定义Option缩减Bitmap生成的时间.以及避免OOM */
                            bitmap = BitmapUtils.getScaledDrawable(image, mAlbumSize, mAlbumSize, Bitmap.Config.RGB_565);
                        }
                        subscriber.onNext(bitmap);
                        subscriber.onCompleted();
                    }
                })
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SubscriberAdapter<Bitmap>() {
                            @Override
                            public void onError(Throwable e) {
                                Logger.w("由于未知的原因ViewDetachedFromWindow导致CircularReveal动画报错,但是不影响最终效果,暂时忽略. " + e);
                            }

                            @Override
                            public void onNext(Bitmap bitmap) {
                                if (bitmap == null) {
                                    /* TODO: 请求网络下载封面 */
                                }
                                updateAlbum(bitmap);
                            }
                        });

                /* TODO 同步歌词 */


            } else {
                /* TODO 联网获取歌曲信息,并存储本地歌曲文件 */

                if (mCurrentId3v2 != null) {
                    mCurrentId3v2 = null;
                    /* 在网络响应后再次调用该方法 */
                    updateAlbum(null);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (mCurrentId3v2 != null) {
                mCurrentId3v2 = null;
                /* 设置默认占位 */
                updateAlbum(null);
            }
        }

        updateProgress();
    }

    /**
     * 通过Pattern获得封面的色调作为头部控件收起时和Toolbar的背景色<br/>
     * 对封面作模糊处理作为头部控件展开时的背景图<br/>
     * 对封面信息做对比,如果是同样的封面(同专辑)就不作处理,避免不必要的开支和可能的延迟<br/>
     *
     * @param bitmap 如果等于null 则恢复默认的色彩和背景
     */
    private void updateAlbum(@Nullable final Bitmap bitmap) {
        if (bitmap == null && mAlbum == mDefAlbum) return;

        Logger.d("准备更新封面 时间 = " + System.currentTimeMillis());
        if (bitmap == null) {
            mAlbum = mDefAlbum;
            mBlurredAlbum = mDefBlurredAlbum;
            mColorTitle = mDefColorTitle;
            mColorMutedBg = mDefColorMutedBg;
            if (mIsExpanded) {
                toggleAlbumBg(true);
            }
        } else {
            mAlbum = new BitmapDrawable(bitmap);

            Palette.Swatch mutedSwatch = from(bitmap).generate().getMutedSwatch();
            if (mutedSwatch != null) {
                mColorTitle = new ColorDrawable(mutedSwatch.getTitleTextColor());
                mColorMutedBg = new ColorDrawable(mutedSwatch.getRgb());
            }

            Observable.create(new Observable.OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {
                    /* 将这种用于处理模糊的Bitmap交给StackBlurManager托管 */
                    Logger.d("背景模糊渲染开始 时间 = " + System.currentTimeMillis());
                    subscriber.onNext(new StackBlurManager(bitmap).process(30));
                    subscriber.onCompleted();
                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SubscriberAdapter<Bitmap>() {
                        @Override
                        public void onNext(Bitmap b) {
                            Logger.d("背景模糊渲染完成 时间 = " + System.currentTimeMillis());
                            mBlurredAlbum = new BitmapDrawable(b);
                            if (mIsExpanded) {
                                toggleAlbumBg(true);
                            }
                        }
                    });
        }

        mIvAlbum.setImageDrawable(mAlbum);
        if (!mIsExpanded) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mIvBgScrim.setVisibility(VISIBLE);
                mIvBgScrim.setImageDrawable(mColorMutedBg);
                Animator animator = ViewAnimationUtils.createCircularReveal(
                        mIvBgScrim,
                        mIvAlbum.getWidth() / 2 + mIvAlbum.getLeft(),
                        mIvBg.getHeight() / 2,
                        0,
                        mIvBg.getWidth());
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setDuration(320);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mIvBg.setImageDrawable(mColorMutedBg);
                        mIvBgScrim.setVisibility(GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mIvBg.setImageDrawable(mColorMutedBg);
                        mIvBgScrim.setVisibility(GONE);
                    }
                });
                animator.start();
            } else {
                toggleAlbumBg(false);
            }
        }
    }

    private void updateProgress() {
        long duration = mPlayer == null ? 0 : mPlayer.getDuration();
        long position = mPlayer == null ? 0 : mPlayer.getCurrentPosition();

        if (duration >= 0) {
            if (mTvDuration != null) {
                mTvDuration.setText(FormatUtils.stringForTime(duration));
            }
            mPbProgress.setMax((int) (duration / 1000));
        }

        mTvProgress.setText(FormatUtils.stringForTime(position));

        mPbProgress.setProgress((int) (position / 1000));

        /* 如果是网络资源(播放地址以http开头)则显示缓存进度 */
        if (mIsOnline) {
            long bufferedPosition = mPlayer == null ? 0 : mPlayer.getBufferedPosition();
            mPbProgress.setSecondaryProgress((int) (bufferedPosition / 1000));
        }

        removeCallbacks(updateProgressAction);

        // Schedule an update if necessary.
        int playbackState = mPlayer == null ? ExoPlayer.STATE_IDLE : mPlayer.getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            long delayMs;
            if (mPlayer.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            postDelayed(updateProgressAction, delayMs);
        }
    }

    /**
     * 计数器回调
     */
    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    @OnClick({R.id.fl_root, R.id.toolbar, R.id.iv_previous_btn, R.id.iv_play_btn, R.id.iv_next_btn, R.id.fac_play_btn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fl_root:
                if (mDragging) return;
                toggleToolbarShown(true);
                break;
            case R.id.toolbar:
                toggleToolbarShown(false);
                break;
            case R.id.iv_previous_btn:
                if (mControlListener != null) {
                    mControlListener.previous();
                }
                break;
            case R.id.iv_play_btn:
            case R.id.fac_play_btn:
                if (mControlListener != null) {
                    /* 判断是播放还是暂停, 回传 */
                    if (mPlayer.getPlayWhenReady()) {
                        mControlListener.pause();
                    } else {
                        mControlListener.play();
                    }
                }
                break;
            case R.id.iv_next_btn:
                if (mControlListener != null) {
                    mControlListener.next();
                }
                break;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(updateProgressAction);
        mVelocityTracker.recycle();
    }

    //==============================================================================================
    ControlListener mControlListener;

    public void setControlListener(ControlListener listener) {
        mControlListener = listener;
    }

    interface ControlListener {
        void previous();

        void next();

        void play();

        void pause();
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    //==============================================================================================
    //=========================================View联动相关=========================================
    //==============================================================================================

    //========================================触摸事件-start======================================

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                break;
            default:
                mDragging = true;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    //========================================NestScroll-start======================================

    /**
     * 滑动开始的调用startNestedScroll()，Parent 收到onStartNestedScroll()回调，
     * 决定是否需要配合 Child 一起进行处理滑动，
     * 如果需要配合,还会回调{@link ControlView#onNestedScrollAccepted(View, View, int)}。
     */
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return true;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        super.onNestedScrollAccepted(child, target, axes);
    }

    /**
     * 每次滑动前，Child 先询问 Parent 是否需要滑动，即dispatchNestedPreScroll()，
     * 这就回调到 Parent 的onNestedPreScroll()，
     * Parent 可以在这个回调中“劫持”掉 Child 的滑动，也就是先于 Child 滑动。
     *
     * @param dx       表示view本次x方向的滚动的总距离长度
     * @param dy       表示view本次y方向的滚动的总距离长度
     * @param consumed 表示父布局消费的距离,consumed[0]表示x方向,consumed[1]表示y方向
     */
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        super.onNestedPreScroll(target, dx, dy, consumed);
        setOffsetBy(dy);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (Math.abs(velocityY) < mMinimumFlingVelocity
                || (velocityY > 0 && mFlRoot.getHeight() <= mHeadMinHeight)
                || (velocityY < 0 && mFlRoot.getHeight() >= mHeadMaxHeight)) {
            return false;
        }

        toggleExpand(velocityY < 0);
        /* 如果想让头部滚动不影响列表滚动,这里应该返回false */
        return false;
    }

    /**
     * 本次滑动结束
     */
    @Override
    public void onStopNestedScroll(View child) {
        super.onStopNestedScroll(child);

        if (mAnimator == null || !mAnimator.isRunning()) {
            toggleExpand(mCurrentHeight > mHeadMinHeight + mMaxOffsetY / 2);
        }
    }

    //========================================NestScroll-end========================================

    private void setOffset(int height) {
        if (height == mFlRoot.getHeight()) return;

        mExpanding = height > mFlRoot.getHeight();

        if (height < mHeadMinHeight) height = mHeadMinHeight;
        if (height > mHeadMaxHeight) height = mHeadMaxHeight;
        mCurrentHeight = height;

        ViewGroup.LayoutParams params = mFlRoot.getLayoutParams();
        params.height = height;
        mFlRoot.setLayoutParams(params);

        ViewGroup.LayoutParams params2 = mIvBg.getLayoutParams();
        params2.height = height + mStatusBarHeight;
        mIvBg.setLayoutParams(params2);

        if (mNestedScrollingChild != null) {
            mNestedScrollingChild.setPadding(0, height + mStatusBarHeight, 0, 0);
        }

        linkViews(getOffsetRadius(height));
    }

    private void setOffsetBy(int dy) {
        if ((dy > 0 && mFlRoot.getHeight() <= mHeadMinHeight) || (dy < 0 && mFlRoot.getHeight() >= mHeadMaxHeight)) {
            return;
        }
        setOffset(mFlRoot.getHeight() - dy);
    }

    private void toggleExpand(boolean expand) {
        int targetY;
        if (expand) {
            targetY = mHeadMaxHeight;
        } else {
            targetY = mHeadMinHeight;
        }

        if (targetY == mCurrentHeight) return;

        if (mAnimator == null) {
            mAnimator = new ValueAnimator();
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (Float) animation.getAnimatedValue();
                    setOffset((int) value);
                }
            });
        } else if (mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        /* 这里的CurrentHeight是上次设置高度是记录的高度值,
        因为上次设置的高度可能还没有被应用,所以这里如果通过mFlRoot.getHeight()来获取高度,可能是不及时的 */
        mAnimator.setFloatValues(mCurrentHeight, targetY);
        float offset = targetY + 0f - mCurrentHeight;
        int duration = Math.min((int) (Math.abs(offset) / mMaxOffsetY * 320), 200);
        mAnimator.setDuration(duration);
        mAnimator.start();
    }

    //========================================其它的联动效果========================================

    private void linkViews(final float offsetRadius) {
        if (mToolbar.getVisibility() == VISIBLE) {
            toggleToolbarShown(false);
        }

        if (mExpanding && !mIsExpanded && offsetRadius > 0.5f) {
            mIsExpanded = true;
        } else if (!mExpanding && mIsExpanded && offsetRadius < 0.5) {
            mIsExpanded = false;
        } else {
            return;
        }

        final int value = mIsExpanded ? 0 : 1;
        if (mIsExpanded) {
            getScaleAlphaAnim(mIvAlbum, value)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            toggleAlbumBg(mIsExpanded);
                            toggleTextOffset(mIsExpanded);
                            toggleControlBtn(mIsExpanded);
                            if (mIsExpanded) {
                                mFacPlayBtn.show();
                            } else {
                                mFacPlayBtn.hide();
                            }
                        }
                    })
                    .start();
        } else {
            toggleAlbumBg(false);
            toggleTextOffset(mIsExpanded);
            toggleControlBtn(mIsExpanded);
            if (mIsExpanded) {
                mFacPlayBtn.show();
            } else {
                mFacPlayBtn.hide();
            }
            getScaleAlphaAnim(mIvAlbum, value)
                    .setListener(null)
                    .start();
        }
    }

    private void toggleTextOffset(boolean expand) {
        controlTextOffsetAnim(mTvTitle, expand ? -mMinLeftMargin : 0);
        controlTextOffsetAnim(mTvArtist, expand ? -mMinLeftMargin : 0);
        controlTextOffsetAnim(mTvProgress, expand ? -mMinLeftMargin : 0);
        controlTextOffsetAnim(mTvDuration, expand ? mDurationRightMargin : 0);
    }

    private void toggleControlBtn(boolean expand) {
        if (expand) {
            controlBtnAnim(mIvNextBtn, 0);
            controlBtnAnim(mIvPlayBtn, 0);
            controlBtnAnim(mIvPreviousBtn, 0);
        } else {
            controlBtnAnim(mIvNextBtn, 1);
            controlBtnAnim(mIvPlayBtn, 1);
            controlBtnAnim(mIvPreviousBtn, 1);
        }
    }

    private void toggleAlbumBg(boolean expand) {
        if (expand) {   // 展开时背景为专辑模糊图片
            Drawable source;
            if (mIvBg.getDrawable() != null) {
                source = mIvBg.getDrawable();
            } else {
                source = getHeadColorDrawable();
            }
            Drawable targetDrawable = getHeadDrawable();
            mIvBg.setImageDrawable(getTransitionDrawable(source, targetDrawable, 380));
        } else {    // 收起时背景为专辑色调纯色
            Drawable source;
            if (mIvBg.getDrawable() != null) {
                source = mIvBg.getDrawable();
            } else {
                source = getHeadDrawable();
            }
            Drawable targetDrawable = getHeadColorDrawable();
            mIvBg.setImageDrawable(getTransitionDrawable(source, targetDrawable, 380));
        }
    }

    private Drawable getHeadDrawable() {
        if (mBlurredAlbum == null) {
            return mDefBlurredAlbum;
        } else {
            return mBlurredAlbum;
        }
    }

    private Drawable getHeadColorDrawable() {
        if (mColorMutedBg == null) {
            return mDefColorMutedBg;
        } else {
            return mColorMutedBg;
        }
    }

    /**
     * 切换为普通Toolbar模式或者歌曲信息模式.<bar/>
     * 注意, 如果当前头部处于展开状态,则不会进行任何切换处理.
     *
     * @param isToolbarShown true 将头部切换为Toolbar,显示导航键,列表名,menu等
     */
    private void toggleToolbarShown(boolean isToolbarShown) {
        if (mIsExpanded || mIsToolbarShown == isToolbarShown) return;
        mIsToolbarShown = isToolbarShown;
        if (isToolbarShown && mToolbar.getVisibility() != VISIBLE) {
            mToolbar.setVisibility(VISIBLE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Animator animator = ViewAnimationUtils.createCircularReveal(
                        mToolbar,
                        mIvAlbum.getWidth() / 2 + mIvAlbum.getLeft(),
                        mToolbar.getHeight() / 2,
                        0,
                        mToolbar.getWidth());
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setDuration(320);
                animator.start();
            } else {
                ViewCompat.animate(mToolbar)
                        .alpha(1)
                        .setDuration(200)
                        .setListener(null)
                        .start();
            }
        } else if (!isToolbarShown && mToolbar.getVisibility() == VISIBLE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Animator animator = ViewAnimationUtils.createCircularReveal(
                        mToolbar,
                        mIvAlbum.getWidth() / 2 + mIvAlbum.getLeft(),
                        mToolbar.getHeight() / 2,
                        mToolbar.getWidth(),
                        0);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setDuration(320);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mToolbar.setVisibility(GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mToolbar.setVisibility(GONE);
                    }
                });
                animator.start();
            } else {
                ViewCompat.animate(mToolbar)
                        .alpha(0)
                        .setDuration(200)
                        .setListener(new ViewPropertyAnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(View view) {
                                super.onAnimationEnd(view);
                                mToolbar.setVisibility(GONE);
                            }
                        })
                        .start();
            }
        }
    }

    //==============================================================================================
    private float getOffsetRadius(int height) {
        float offsetY = height - mHeadMinHeight;
        return offsetY / mMaxOffsetY;
    }

    private void controlBtnAnim(View v, float value) {
        getScaleAlphaAnim(v, value).start();
    }

    private ViewPropertyAnimatorCompat getScaleAlphaAnim(View v, float value) {
        return ViewCompat.animate(v)
                .scaleX(value)
                .scaleY(value)
                .alpha(value)
                .setDuration(240);
    }

    private void controlTextOffsetAnim(View v, int value) {
        ViewCompat.animate(v)
                .translationX(value)
                .setDuration(240)
                .start();
    }

    private static TransitionDrawable getTransitionDrawable(Drawable source, Drawable target, int duration) {
        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{source, target});
        transitionDrawable.setCrossFadeEnabled(true);
        transitionDrawable.startTransition(duration);
        return transitionDrawable;
    }
}
