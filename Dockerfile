FROM ubuntu:22.04

# Install dependencies
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set Java home
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Install Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

RUN mkdir -p $ANDROID_HOME && \
    cd $ANDROID_HOME && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip -q commandlinetools-linux-9477386_latest.zip && \
    rm commandlinetools-linux-9477386_latest.zip && \
    mv cmdline-tools latest && \
    mkdir -p cmdline-tools && \
    mv latest cmdline-tools/

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses && \
    sdkmanager \
    "platforms;android-35" \
    "build-tools;35.0.0" \
    "platform-tools" \
    "tools"

# Set working directory
WORKDIR /workspace

# Copy project files
COPY . .

# Make gradlew executable
RUN chmod +x gradlew

# Build APK
CMD ["./gradlew", "assembleRelease"]
