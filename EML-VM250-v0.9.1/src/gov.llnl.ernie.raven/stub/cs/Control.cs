// <auto-generated>
//     Generated by the protocol buffer compiler.  DO NOT EDIT!
//     source: gov/llnl/ernie/raven/proto/control.proto
// </auto-generated>
#pragma warning disable 1591, 0612, 3021
#region Designer generated code

using pb = global::Google.Protobuf;
using pbc = global::Google.Protobuf.Collections;
using pbr = global::Google.Protobuf.Reflection;
using scg = global::System.Collections.Generic;
namespace Gov.Llnl.Ernie.Raven {

  /// <summary>Holder for reflection information generated from gov/llnl/ernie/raven/proto/control.proto</summary>
  public static partial class ControlReflection {

    #region Descriptor
    /// <summary>File descriptor for gov/llnl/ernie/raven/proto/control.proto</summary>
    public static pbr::FileDescriptor Descriptor {
      get { return descriptor; }
    }
    private static pbr::FileDescriptor descriptor;

    static ControlReflection() {
      byte[] descriptorData = global::System.Convert.FromBase64String(
          string.Concat(
            "Cihnb3YvbGxubC9lcm5pZS9yYXZlbi9wcm90by9jb250cm9sLnByb3RvEhRn",
            "b3YubGxubC5lcm5pZS5yYXZlbiIgCg1FcnJvclJlc3BvbnNlEg8KB21lc3Nh",
            "Z2UYASABKAkiIQoQQ29uZmlndXJlUmVxdWVzdBINCgVsYW5lcxgBIAEoBSIh",
            "ChFDb25maWd1cmVSZXNwb25zZRIMCgRwb3J0GAEgAygFIg8KDVN0YXR1c1Jl",
            "cXVlc3QigAEKDlN0YXR1c1Jlc3BvbnNlEg8KB3ZlcnNpb24YASABKAkSFAoM",
            "Y29udHJvbF9wb3J0GAIgASgFEhAKCGxvZ19wb3J0GAMgASgFEjUKC2xhbmVf",
            "c3RhdHVzGAQgAygLMiAuZ292LmxsbmwuZXJuaWUucmF2ZW4uTGFuZVN0YXR1",
            "cyJCCgpMYW5lU3RhdHVzEgwKBHBvcnQYASABKAUSEAoIbWVzc2FnZXMYAiAB",
            "KAUSFAoMbGFzdF9zY2FuX2lkGAMgASgFIhIKEFRlcm1pbmF0ZVJlcXVlc3Qi",
            "EwoRVGVybWluYXRlUmVzcG9uc2VCJQoUZ292LmxsbmwuZXJuaWUucmF2ZW5C",
            "DUNvbnRyb2xQcm90b3NiBnByb3RvMw=="));
      descriptor = pbr::FileDescriptor.FromGeneratedCode(descriptorData,
          new pbr::FileDescriptor[] { },
          new pbr::GeneratedClrTypeInfo(null, null, new pbr::GeneratedClrTypeInfo[] {
            new pbr::GeneratedClrTypeInfo(typeof(global::Gov.Llnl.Ernie.Raven.ErrorResponse), global::Gov.Llnl.Ernie.Raven.ErrorResponse.Parser, new[]{ "Message" }, null, null, null, null),
            new pbr::GeneratedClrTypeInfo(typeof(global::Gov.Llnl.Ernie.Raven.ConfigureRequest), global::Gov.Llnl.Ernie.Raven.ConfigureRequest.Parser, new[]{ "Lanes" }, null, null, null, null),
            new pbr::GeneratedClrTypeInfo(typeof(global::Gov.Llnl.Ernie.Raven.ConfigureResponse), global::Gov.Llnl.Ernie.Raven.ConfigureResponse.Parser, new[]{ "Port" }, null, null, null, null),
            new pbr::GeneratedClrTypeInfo(typeof(global::Gov.Llnl.Ernie.Raven.StatusRequest), global::Gov.Llnl.Ernie.Raven.StatusRequest.Parser, null, null, null, null, null),
            new pbr::GeneratedClrTypeInfo(typeof(global::Gov.Llnl.Ernie.Raven.StatusResponse), global::Gov.Llnl.Ernie.Raven.StatusResponse.Parser, new[]{ "Version", "ControlPort", "LogPort", "LaneStatus" }, null, null, null, null),
            new pbr::GeneratedClrTypeInfo(typeof(global::Gov.Llnl.Ernie.Raven.LaneStatus), global::Gov.Llnl.Ernie.Raven.LaneStatus.Parser, new[]{ "Port", "Messages", "LastScanId" }, null, null, null, null),
            new pbr::GeneratedClrTypeInfo(typeof(global::Gov.Llnl.Ernie.Raven.TerminateRequest), global::Gov.Llnl.Ernie.Raven.TerminateRequest.Parser, null, null, null, null, null),
            new pbr::GeneratedClrTypeInfo(typeof(global::Gov.Llnl.Ernie.Raven.TerminateResponse), global::Gov.Llnl.Ernie.Raven.TerminateResponse.Parser, null, null, null, null, null)
          }));
    }
    #endregion

  }
  #region Messages
  public sealed partial class ErrorResponse : pb::IMessage<ErrorResponse> {
    private static readonly pb::MessageParser<ErrorResponse> _parser = new pb::MessageParser<ErrorResponse>(() => new ErrorResponse());
    private pb::UnknownFieldSet _unknownFields;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pb::MessageParser<ErrorResponse> Parser { get { return _parser; } }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pbr::MessageDescriptor Descriptor {
      get { return global::Gov.Llnl.Ernie.Raven.ControlReflection.Descriptor.MessageTypes[0]; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    pbr::MessageDescriptor pb::IMessage.Descriptor {
      get { return Descriptor; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ErrorResponse() {
      OnConstruction();
    }

    partial void OnConstruction();

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ErrorResponse(ErrorResponse other) : this() {
      message_ = other.message_;
      _unknownFields = pb::UnknownFieldSet.Clone(other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ErrorResponse Clone() {
      return new ErrorResponse(this);
    }

    /// <summary>Field number for the "message" field.</summary>
    public const int MessageFieldNumber = 1;
    private string message_ = "";
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public string Message {
      get { return message_; }
      set {
        message_ = pb::ProtoPreconditions.CheckNotNull(value, "value");
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override bool Equals(object other) {
      return Equals(other as ErrorResponse);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public bool Equals(ErrorResponse other) {
      if (ReferenceEquals(other, null)) {
        return false;
      }
      if (ReferenceEquals(other, this)) {
        return true;
      }
      if (Message != other.Message) return false;
      return Equals(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override int GetHashCode() {
      int hash = 1;
      if (Message.Length != 0) hash ^= Message.GetHashCode();
      if (_unknownFields != null) {
        hash ^= _unknownFields.GetHashCode();
      }
      return hash;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override string ToString() {
      return pb::JsonFormatter.ToDiagnosticString(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void WriteTo(pb::CodedOutputStream output) {
      if (Message.Length != 0) {
        output.WriteRawTag(10);
        output.WriteString(Message);
      }
      if (_unknownFields != null) {
        _unknownFields.WriteTo(output);
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int CalculateSize() {
      int size = 0;
      if (Message.Length != 0) {
        size += 1 + pb::CodedOutputStream.ComputeStringSize(Message);
      }
      if (_unknownFields != null) {
        size += _unknownFields.CalculateSize();
      }
      return size;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(ErrorResponse other) {
      if (other == null) {
        return;
      }
      if (other.Message.Length != 0) {
        Message = other.Message;
      }
      _unknownFields = pb::UnknownFieldSet.MergeFrom(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(pb::CodedInputStream input) {
      uint tag;
      while ((tag = input.ReadTag()) != 0) {
        switch(tag) {
          default:
            _unknownFields = pb::UnknownFieldSet.MergeFieldFrom(_unknownFields, input);
            break;
          case 10: {
            Message = input.ReadString();
            break;
          }
        }
      }
    }

  }

  public sealed partial class ConfigureRequest : pb::IMessage<ConfigureRequest> {
    private static readonly pb::MessageParser<ConfigureRequest> _parser = new pb::MessageParser<ConfigureRequest>(() => new ConfigureRequest());
    private pb::UnknownFieldSet _unknownFields;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pb::MessageParser<ConfigureRequest> Parser { get { return _parser; } }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pbr::MessageDescriptor Descriptor {
      get { return global::Gov.Llnl.Ernie.Raven.ControlReflection.Descriptor.MessageTypes[1]; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    pbr::MessageDescriptor pb::IMessage.Descriptor {
      get { return Descriptor; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ConfigureRequest() {
      OnConstruction();
    }

    partial void OnConstruction();

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ConfigureRequest(ConfigureRequest other) : this() {
      lanes_ = other.lanes_;
      _unknownFields = pb::UnknownFieldSet.Clone(other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ConfigureRequest Clone() {
      return new ConfigureRequest(this);
    }

    /// <summary>Field number for the "lanes" field.</summary>
    public const int LanesFieldNumber = 1;
    private int lanes_;
    /// <summary>
    /// How many lanes 
    /// </summary>
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int Lanes {
      get { return lanes_; }
      set {
        lanes_ = value;
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override bool Equals(object other) {
      return Equals(other as ConfigureRequest);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public bool Equals(ConfigureRequest other) {
      if (ReferenceEquals(other, null)) {
        return false;
      }
      if (ReferenceEquals(other, this)) {
        return true;
      }
      if (Lanes != other.Lanes) return false;
      return Equals(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override int GetHashCode() {
      int hash = 1;
      if (Lanes != 0) hash ^= Lanes.GetHashCode();
      if (_unknownFields != null) {
        hash ^= _unknownFields.GetHashCode();
      }
      return hash;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override string ToString() {
      return pb::JsonFormatter.ToDiagnosticString(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void WriteTo(pb::CodedOutputStream output) {
      if (Lanes != 0) {
        output.WriteRawTag(8);
        output.WriteInt32(Lanes);
      }
      if (_unknownFields != null) {
        _unknownFields.WriteTo(output);
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int CalculateSize() {
      int size = 0;
      if (Lanes != 0) {
        size += 1 + pb::CodedOutputStream.ComputeInt32Size(Lanes);
      }
      if (_unknownFields != null) {
        size += _unknownFields.CalculateSize();
      }
      return size;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(ConfigureRequest other) {
      if (other == null) {
        return;
      }
      if (other.Lanes != 0) {
        Lanes = other.Lanes;
      }
      _unknownFields = pb::UnknownFieldSet.MergeFrom(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(pb::CodedInputStream input) {
      uint tag;
      while ((tag = input.ReadTag()) != 0) {
        switch(tag) {
          default:
            _unknownFields = pb::UnknownFieldSet.MergeFieldFrom(_unknownFields, input);
            break;
          case 8: {
            Lanes = input.ReadInt32();
            break;
          }
        }
      }
    }

  }

  public sealed partial class ConfigureResponse : pb::IMessage<ConfigureResponse> {
    private static readonly pb::MessageParser<ConfigureResponse> _parser = new pb::MessageParser<ConfigureResponse>(() => new ConfigureResponse());
    private pb::UnknownFieldSet _unknownFields;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pb::MessageParser<ConfigureResponse> Parser { get { return _parser; } }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pbr::MessageDescriptor Descriptor {
      get { return global::Gov.Llnl.Ernie.Raven.ControlReflection.Descriptor.MessageTypes[2]; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    pbr::MessageDescriptor pb::IMessage.Descriptor {
      get { return Descriptor; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ConfigureResponse() {
      OnConstruction();
    }

    partial void OnConstruction();

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ConfigureResponse(ConfigureResponse other) : this() {
      port_ = other.port_.Clone();
      _unknownFields = pb::UnknownFieldSet.Clone(other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public ConfigureResponse Clone() {
      return new ConfigureResponse(this);
    }

    /// <summary>Field number for the "port" field.</summary>
    public const int PortFieldNumber = 1;
    private static readonly pb::FieldCodec<int> _repeated_port_codec
        = pb::FieldCodec.ForInt32(10);
    private readonly pbc::RepeatedField<int> port_ = new pbc::RepeatedField<int>();
    /// <summary>
    /// A list of ports for each lanes
    /// </summary>
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public pbc::RepeatedField<int> Port {
      get { return port_; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override bool Equals(object other) {
      return Equals(other as ConfigureResponse);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public bool Equals(ConfigureResponse other) {
      if (ReferenceEquals(other, null)) {
        return false;
      }
      if (ReferenceEquals(other, this)) {
        return true;
      }
      if(!port_.Equals(other.port_)) return false;
      return Equals(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override int GetHashCode() {
      int hash = 1;
      hash ^= port_.GetHashCode();
      if (_unknownFields != null) {
        hash ^= _unknownFields.GetHashCode();
      }
      return hash;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override string ToString() {
      return pb::JsonFormatter.ToDiagnosticString(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void WriteTo(pb::CodedOutputStream output) {
      port_.WriteTo(output, _repeated_port_codec);
      if (_unknownFields != null) {
        _unknownFields.WriteTo(output);
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int CalculateSize() {
      int size = 0;
      size += port_.CalculateSize(_repeated_port_codec);
      if (_unknownFields != null) {
        size += _unknownFields.CalculateSize();
      }
      return size;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(ConfigureResponse other) {
      if (other == null) {
        return;
      }
      port_.Add(other.port_);
      _unknownFields = pb::UnknownFieldSet.MergeFrom(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(pb::CodedInputStream input) {
      uint tag;
      while ((tag = input.ReadTag()) != 0) {
        switch(tag) {
          default:
            _unknownFields = pb::UnknownFieldSet.MergeFieldFrom(_unknownFields, input);
            break;
          case 10:
          case 8: {
            port_.AddEntriesFrom(input, _repeated_port_codec);
            break;
          }
        }
      }
    }

  }

  public sealed partial class StatusRequest : pb::IMessage<StatusRequest> {
    private static readonly pb::MessageParser<StatusRequest> _parser = new pb::MessageParser<StatusRequest>(() => new StatusRequest());
    private pb::UnknownFieldSet _unknownFields;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pb::MessageParser<StatusRequest> Parser { get { return _parser; } }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pbr::MessageDescriptor Descriptor {
      get { return global::Gov.Llnl.Ernie.Raven.ControlReflection.Descriptor.MessageTypes[3]; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    pbr::MessageDescriptor pb::IMessage.Descriptor {
      get { return Descriptor; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public StatusRequest() {
      OnConstruction();
    }

    partial void OnConstruction();

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public StatusRequest(StatusRequest other) : this() {
      _unknownFields = pb::UnknownFieldSet.Clone(other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public StatusRequest Clone() {
      return new StatusRequest(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override bool Equals(object other) {
      return Equals(other as StatusRequest);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public bool Equals(StatusRequest other) {
      if (ReferenceEquals(other, null)) {
        return false;
      }
      if (ReferenceEquals(other, this)) {
        return true;
      }
      return Equals(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override int GetHashCode() {
      int hash = 1;
      if (_unknownFields != null) {
        hash ^= _unknownFields.GetHashCode();
      }
      return hash;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override string ToString() {
      return pb::JsonFormatter.ToDiagnosticString(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void WriteTo(pb::CodedOutputStream output) {
      if (_unknownFields != null) {
        _unknownFields.WriteTo(output);
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int CalculateSize() {
      int size = 0;
      if (_unknownFields != null) {
        size += _unknownFields.CalculateSize();
      }
      return size;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(StatusRequest other) {
      if (other == null) {
        return;
      }
      _unknownFields = pb::UnknownFieldSet.MergeFrom(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(pb::CodedInputStream input) {
      uint tag;
      while ((tag = input.ReadTag()) != 0) {
        switch(tag) {
          default:
            _unknownFields = pb::UnknownFieldSet.MergeFieldFrom(_unknownFields, input);
            break;
        }
      }
    }

  }

  public sealed partial class StatusResponse : pb::IMessage<StatusResponse> {
    private static readonly pb::MessageParser<StatusResponse> _parser = new pb::MessageParser<StatusResponse>(() => new StatusResponse());
    private pb::UnknownFieldSet _unknownFields;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pb::MessageParser<StatusResponse> Parser { get { return _parser; } }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pbr::MessageDescriptor Descriptor {
      get { return global::Gov.Llnl.Ernie.Raven.ControlReflection.Descriptor.MessageTypes[4]; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    pbr::MessageDescriptor pb::IMessage.Descriptor {
      get { return Descriptor; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public StatusResponse() {
      OnConstruction();
    }

    partial void OnConstruction();

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public StatusResponse(StatusResponse other) : this() {
      version_ = other.version_;
      controlPort_ = other.controlPort_;
      logPort_ = other.logPort_;
      laneStatus_ = other.laneStatus_.Clone();
      _unknownFields = pb::UnknownFieldSet.Clone(other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public StatusResponse Clone() {
      return new StatusResponse(this);
    }

    /// <summary>Field number for the "version" field.</summary>
    public const int VersionFieldNumber = 1;
    private string version_ = "";
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public string Version {
      get { return version_; }
      set {
        version_ = pb::ProtoPreconditions.CheckNotNull(value, "value");
      }
    }

    /// <summary>Field number for the "control_port" field.</summary>
    public const int ControlPortFieldNumber = 2;
    private int controlPort_;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int ControlPort {
      get { return controlPort_; }
      set {
        controlPort_ = value;
      }
    }

    /// <summary>Field number for the "log_port" field.</summary>
    public const int LogPortFieldNumber = 3;
    private int logPort_;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int LogPort {
      get { return logPort_; }
      set {
        logPort_ = value;
      }
    }

    /// <summary>Field number for the "lane_status" field.</summary>
    public const int LaneStatusFieldNumber = 4;
    private static readonly pb::FieldCodec<global::Gov.Llnl.Ernie.Raven.LaneStatus> _repeated_laneStatus_codec
        = pb::FieldCodec.ForMessage(34, global::Gov.Llnl.Ernie.Raven.LaneStatus.Parser);
    private readonly pbc::RepeatedField<global::Gov.Llnl.Ernie.Raven.LaneStatus> laneStatus_ = new pbc::RepeatedField<global::Gov.Llnl.Ernie.Raven.LaneStatus>();
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public pbc::RepeatedField<global::Gov.Llnl.Ernie.Raven.LaneStatus> LaneStatus {
      get { return laneStatus_; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override bool Equals(object other) {
      return Equals(other as StatusResponse);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public bool Equals(StatusResponse other) {
      if (ReferenceEquals(other, null)) {
        return false;
      }
      if (ReferenceEquals(other, this)) {
        return true;
      }
      if (Version != other.Version) return false;
      if (ControlPort != other.ControlPort) return false;
      if (LogPort != other.LogPort) return false;
      if(!laneStatus_.Equals(other.laneStatus_)) return false;
      return Equals(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override int GetHashCode() {
      int hash = 1;
      if (Version.Length != 0) hash ^= Version.GetHashCode();
      if (ControlPort != 0) hash ^= ControlPort.GetHashCode();
      if (LogPort != 0) hash ^= LogPort.GetHashCode();
      hash ^= laneStatus_.GetHashCode();
      if (_unknownFields != null) {
        hash ^= _unknownFields.GetHashCode();
      }
      return hash;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override string ToString() {
      return pb::JsonFormatter.ToDiagnosticString(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void WriteTo(pb::CodedOutputStream output) {
      if (Version.Length != 0) {
        output.WriteRawTag(10);
        output.WriteString(Version);
      }
      if (ControlPort != 0) {
        output.WriteRawTag(16);
        output.WriteInt32(ControlPort);
      }
      if (LogPort != 0) {
        output.WriteRawTag(24);
        output.WriteInt32(LogPort);
      }
      laneStatus_.WriteTo(output, _repeated_laneStatus_codec);
      if (_unknownFields != null) {
        _unknownFields.WriteTo(output);
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int CalculateSize() {
      int size = 0;
      if (Version.Length != 0) {
        size += 1 + pb::CodedOutputStream.ComputeStringSize(Version);
      }
      if (ControlPort != 0) {
        size += 1 + pb::CodedOutputStream.ComputeInt32Size(ControlPort);
      }
      if (LogPort != 0) {
        size += 1 + pb::CodedOutputStream.ComputeInt32Size(LogPort);
      }
      size += laneStatus_.CalculateSize(_repeated_laneStatus_codec);
      if (_unknownFields != null) {
        size += _unknownFields.CalculateSize();
      }
      return size;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(StatusResponse other) {
      if (other == null) {
        return;
      }
      if (other.Version.Length != 0) {
        Version = other.Version;
      }
      if (other.ControlPort != 0) {
        ControlPort = other.ControlPort;
      }
      if (other.LogPort != 0) {
        LogPort = other.LogPort;
      }
      laneStatus_.Add(other.laneStatus_);
      _unknownFields = pb::UnknownFieldSet.MergeFrom(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(pb::CodedInputStream input) {
      uint tag;
      while ((tag = input.ReadTag()) != 0) {
        switch(tag) {
          default:
            _unknownFields = pb::UnknownFieldSet.MergeFieldFrom(_unknownFields, input);
            break;
          case 10: {
            Version = input.ReadString();
            break;
          }
          case 16: {
            ControlPort = input.ReadInt32();
            break;
          }
          case 24: {
            LogPort = input.ReadInt32();
            break;
          }
          case 34: {
            laneStatus_.AddEntriesFrom(input, _repeated_laneStatus_codec);
            break;
          }
        }
      }
    }

  }

  public sealed partial class LaneStatus : pb::IMessage<LaneStatus> {
    private static readonly pb::MessageParser<LaneStatus> _parser = new pb::MessageParser<LaneStatus>(() => new LaneStatus());
    private pb::UnknownFieldSet _unknownFields;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pb::MessageParser<LaneStatus> Parser { get { return _parser; } }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pbr::MessageDescriptor Descriptor {
      get { return global::Gov.Llnl.Ernie.Raven.ControlReflection.Descriptor.MessageTypes[5]; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    pbr::MessageDescriptor pb::IMessage.Descriptor {
      get { return Descriptor; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public LaneStatus() {
      OnConstruction();
    }

    partial void OnConstruction();

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public LaneStatus(LaneStatus other) : this() {
      port_ = other.port_;
      messages_ = other.messages_;
      lastScanId_ = other.lastScanId_;
      _unknownFields = pb::UnknownFieldSet.Clone(other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public LaneStatus Clone() {
      return new LaneStatus(this);
    }

    /// <summary>Field number for the "port" field.</summary>
    public const int PortFieldNumber = 1;
    private int port_;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int Port {
      get { return port_; }
      set {
        port_ = value;
      }
    }

    /// <summary>Field number for the "messages" field.</summary>
    public const int MessagesFieldNumber = 2;
    private int messages_;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int Messages {
      get { return messages_; }
      set {
        messages_ = value;
      }
    }

    /// <summary>Field number for the "last_scan_id" field.</summary>
    public const int LastScanIdFieldNumber = 3;
    private int lastScanId_;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int LastScanId {
      get { return lastScanId_; }
      set {
        lastScanId_ = value;
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override bool Equals(object other) {
      return Equals(other as LaneStatus);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public bool Equals(LaneStatus other) {
      if (ReferenceEquals(other, null)) {
        return false;
      }
      if (ReferenceEquals(other, this)) {
        return true;
      }
      if (Port != other.Port) return false;
      if (Messages != other.Messages) return false;
      if (LastScanId != other.LastScanId) return false;
      return Equals(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override int GetHashCode() {
      int hash = 1;
      if (Port != 0) hash ^= Port.GetHashCode();
      if (Messages != 0) hash ^= Messages.GetHashCode();
      if (LastScanId != 0) hash ^= LastScanId.GetHashCode();
      if (_unknownFields != null) {
        hash ^= _unknownFields.GetHashCode();
      }
      return hash;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override string ToString() {
      return pb::JsonFormatter.ToDiagnosticString(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void WriteTo(pb::CodedOutputStream output) {
      if (Port != 0) {
        output.WriteRawTag(8);
        output.WriteInt32(Port);
      }
      if (Messages != 0) {
        output.WriteRawTag(16);
        output.WriteInt32(Messages);
      }
      if (LastScanId != 0) {
        output.WriteRawTag(24);
        output.WriteInt32(LastScanId);
      }
      if (_unknownFields != null) {
        _unknownFields.WriteTo(output);
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int CalculateSize() {
      int size = 0;
      if (Port != 0) {
        size += 1 + pb::CodedOutputStream.ComputeInt32Size(Port);
      }
      if (Messages != 0) {
        size += 1 + pb::CodedOutputStream.ComputeInt32Size(Messages);
      }
      if (LastScanId != 0) {
        size += 1 + pb::CodedOutputStream.ComputeInt32Size(LastScanId);
      }
      if (_unknownFields != null) {
        size += _unknownFields.CalculateSize();
      }
      return size;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(LaneStatus other) {
      if (other == null) {
        return;
      }
      if (other.Port != 0) {
        Port = other.Port;
      }
      if (other.Messages != 0) {
        Messages = other.Messages;
      }
      if (other.LastScanId != 0) {
        LastScanId = other.LastScanId;
      }
      _unknownFields = pb::UnknownFieldSet.MergeFrom(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(pb::CodedInputStream input) {
      uint tag;
      while ((tag = input.ReadTag()) != 0) {
        switch(tag) {
          default:
            _unknownFields = pb::UnknownFieldSet.MergeFieldFrom(_unknownFields, input);
            break;
          case 8: {
            Port = input.ReadInt32();
            break;
          }
          case 16: {
            Messages = input.ReadInt32();
            break;
          }
          case 24: {
            LastScanId = input.ReadInt32();
            break;
          }
        }
      }
    }

  }

  public sealed partial class TerminateRequest : pb::IMessage<TerminateRequest> {
    private static readonly pb::MessageParser<TerminateRequest> _parser = new pb::MessageParser<TerminateRequest>(() => new TerminateRequest());
    private pb::UnknownFieldSet _unknownFields;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pb::MessageParser<TerminateRequest> Parser { get { return _parser; } }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pbr::MessageDescriptor Descriptor {
      get { return global::Gov.Llnl.Ernie.Raven.ControlReflection.Descriptor.MessageTypes[6]; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    pbr::MessageDescriptor pb::IMessage.Descriptor {
      get { return Descriptor; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public TerminateRequest() {
      OnConstruction();
    }

    partial void OnConstruction();

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public TerminateRequest(TerminateRequest other) : this() {
      _unknownFields = pb::UnknownFieldSet.Clone(other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public TerminateRequest Clone() {
      return new TerminateRequest(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override bool Equals(object other) {
      return Equals(other as TerminateRequest);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public bool Equals(TerminateRequest other) {
      if (ReferenceEquals(other, null)) {
        return false;
      }
      if (ReferenceEquals(other, this)) {
        return true;
      }
      return Equals(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override int GetHashCode() {
      int hash = 1;
      if (_unknownFields != null) {
        hash ^= _unknownFields.GetHashCode();
      }
      return hash;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override string ToString() {
      return pb::JsonFormatter.ToDiagnosticString(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void WriteTo(pb::CodedOutputStream output) {
      if (_unknownFields != null) {
        _unknownFields.WriteTo(output);
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int CalculateSize() {
      int size = 0;
      if (_unknownFields != null) {
        size += _unknownFields.CalculateSize();
      }
      return size;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(TerminateRequest other) {
      if (other == null) {
        return;
      }
      _unknownFields = pb::UnknownFieldSet.MergeFrom(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(pb::CodedInputStream input) {
      uint tag;
      while ((tag = input.ReadTag()) != 0) {
        switch(tag) {
          default:
            _unknownFields = pb::UnknownFieldSet.MergeFieldFrom(_unknownFields, input);
            break;
        }
      }
    }

  }

  public sealed partial class TerminateResponse : pb::IMessage<TerminateResponse> {
    private static readonly pb::MessageParser<TerminateResponse> _parser = new pb::MessageParser<TerminateResponse>(() => new TerminateResponse());
    private pb::UnknownFieldSet _unknownFields;
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pb::MessageParser<TerminateResponse> Parser { get { return _parser; } }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public static pbr::MessageDescriptor Descriptor {
      get { return global::Gov.Llnl.Ernie.Raven.ControlReflection.Descriptor.MessageTypes[7]; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    pbr::MessageDescriptor pb::IMessage.Descriptor {
      get { return Descriptor; }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public TerminateResponse() {
      OnConstruction();
    }

    partial void OnConstruction();

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public TerminateResponse(TerminateResponse other) : this() {
      _unknownFields = pb::UnknownFieldSet.Clone(other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public TerminateResponse Clone() {
      return new TerminateResponse(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override bool Equals(object other) {
      return Equals(other as TerminateResponse);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public bool Equals(TerminateResponse other) {
      if (ReferenceEquals(other, null)) {
        return false;
      }
      if (ReferenceEquals(other, this)) {
        return true;
      }
      return Equals(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override int GetHashCode() {
      int hash = 1;
      if (_unknownFields != null) {
        hash ^= _unknownFields.GetHashCode();
      }
      return hash;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public override string ToString() {
      return pb::JsonFormatter.ToDiagnosticString(this);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void WriteTo(pb::CodedOutputStream output) {
      if (_unknownFields != null) {
        _unknownFields.WriteTo(output);
      }
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public int CalculateSize() {
      int size = 0;
      if (_unknownFields != null) {
        size += _unknownFields.CalculateSize();
      }
      return size;
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(TerminateResponse other) {
      if (other == null) {
        return;
      }
      _unknownFields = pb::UnknownFieldSet.MergeFrom(_unknownFields, other._unknownFields);
    }

    [global::System.Diagnostics.DebuggerNonUserCodeAttribute]
    public void MergeFrom(pb::CodedInputStream input) {
      uint tag;
      while ((tag = input.ReadTag()) != 0) {
        switch(tag) {
          default:
            _unknownFields = pb::UnknownFieldSet.MergeFieldFrom(_unknownFields, input);
            break;
        }
      }
    }

  }

  #endregion

}

#endregion Designer generated code